package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.KpSystemPrompt
import com.kreditpintar.chatbot.config.MiniMaxClient
import com.kreditpintar.chatbot.pipeline.ContextAssembler
import com.kreditpintar.chatbot.pipeline.ValuesFilterService
import com.kreditpintar.chatbot.pipeline.VectorSearchResult
import com.kreditpintar.chatbot.pipeline.VectorSearchService
import com.kreditpintar.chatbot.pipeline.WebSearchEntry
import com.kreditpintar.chatbot.pipeline.WebSearchService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class ChatMessageResponse(
    val response: String,
    val sources: List<SourceInfo>,
    val sessionId: UUID
)

data class SourceInfo(
    val source: String,
    val type: String
)

@Service
class ChatService(
    private val miniMaxClient: MiniMaxClient,
    private val vectorSearchService: VectorSearchService,
    private val webSearchService: WebSearchService,
    private val valuesFilterService: ValuesFilterService,
    private val contextAssembler: ContextAssembler,
    private val outputValidatorService: OutputValidatorService,
    private val sessionService: SessionService,
    private val properties: ChatbotProperties
) {
    fun chat(sessionId: UUID, userMessage: String): ChatMessageResponse {
        // Step 1: Get conversation history
        val history = sessionService.getConversationHistory(sessionId)
        val historyPairs = history.map { msg ->
            Pair(msg.role.lowercase(), msg.content)
        }

        // Step 2: Vector similarity search on document knowledge
        val docResults: List<VectorSearchResult> = try {
            vectorSearchService.search(userMessage)
        } catch (e: Exception) {
            logger.error(e) { "Vector search failed for session=$sessionId" }
            emptyList()
        }

        // Step 3: Web search (fallback or supplement when docs are sparse)
        val webResults: List<WebSearchEntry> = if (docResults.isEmpty() || docResults.size < 2) {
            try {
                webSearchService.search(userMessage, historyPairs)
            } catch (e: Exception) {
                logger.error(e) { "Web search failed for session=$sessionId" }
                emptyList()
            }
        } else {
            emptyList()
        }

        // Step 4: Values filter on web results (NEVER skip this per AGENTS.md)
        val filteredWebResults = if (webResults.isNotEmpty()) {
            valuesFilterService.filter(webResults, sessionId.toString())
        } else {
            emptyList()
        }

        // Step 5: Assemble context
        val assembledContext = contextAssembler.assemble(docResults, filteredWebResults)

        // Step 6: Generate response via MiniMax
        val chatResult = try {
            miniMaxClient.chat(
                systemPrompt = KpSystemPrompt.SYSTEM_PROMPT,
                context = assembledContext.context,
                history = historyPairs,
                userMessage = userMessage
            )
        } catch (e: Exception) {
            logger.error(e) { "Chat generation failed for session=$sessionId" }
            return ChatMessageResponse(
                response = properties.fallbackMessage,
                sources = emptyList(),
                sessionId = sessionId
            )
        }

        // Step 7: Output validation (NEVER skip this per AGENTS.md)
        val validationResult = outputValidatorService.validate(chatResult.content, sessionId.toString())

        val finalResponse = if (validationResult.passed) {
            chatResult.content
        } else {
            logger.warn { "Output validation failed for session=$sessionId: ${validationResult.reason}" }
            properties.fallbackMessage
        }

        // Step 8: Persist messages
        sessionService.saveMessage(sessionId, "user", userMessage)
        sessionService.saveMessage(sessionId, "assistant", finalResponse)

        // Step 9: Build response with citations
        val sources = assembledContext.citations.map {
            SourceInfo(source = it.source, type = it.type)
        }

        return ChatMessageResponse(
            response = finalResponse,
            sources = sources,
            sessionId = sessionId
        )
    }
}

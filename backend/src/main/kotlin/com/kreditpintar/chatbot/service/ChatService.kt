package com.kreditpintar.chatbot.service

import com.fasterxml.jackson.annotation.JsonProperty
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
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

data class ChatMessageResponse(
    @JsonProperty("session_id") val sessionId: UUID,
    @JsonProperty("response") val response: String,
    @JsonProperty("citations") val citations: List<SourceInfo>,
    @JsonProperty("response_time_ms") val responseTimeMs: Long,
    @JsonProperty("timestamp") val timestamp: String,
)

data class SourceInfo(
    @JsonProperty("source") val source: String,
    @JsonProperty("type") val type: String,
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
    private val properties: ChatbotProperties,
) {
    fun chat(
        sessionId: UUID,
        userMessage: String,
    ): ChatMessageResponse {
        val startTime = System.currentTimeMillis()

        // Step 1 + 2a: Fetch history and embed query in parallel
        val historyFuture = CompletableFuture.supplyAsync {
            sessionService.getConversationHistory(sessionId)
        }
        val docResultsFuture = CompletableFuture.supplyAsync {
            try {
                vectorSearchService.search(userMessage)
            } catch (e: Exception) {
                logger.error(e) { "Vector search failed for session=$sessionId" }
                emptyList<VectorSearchResult>()
            }
        }

        val historyPairs = historyFuture.get().map { Pair(it.role.lowercase(), it.content) }
        val docResults: List<VectorSearchResult> = docResultsFuture.get()

        // Step 3: Web search (fallback or supplement when docs are sparse)
        val webResults: List<WebSearchEntry> =
            if (docResults.isEmpty() || docResults.size < 2) {
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
        val filteredWebResults =
            if (webResults.isNotEmpty()) {
                valuesFilterService.filter(webResults, sessionId.toString())
            } else {
                emptyList()
            }

        // Step 5: Assemble context
        val assembledContext = contextAssembler.assemble(docResults, filteredWebResults)

        // Step 6: Generate response via MiniMax
        val chatResult =
            try {
                miniMaxClient.chat(
                    systemPrompt = KpSystemPrompt.SYSTEM_PROMPT,
                    context = assembledContext.context,
                    history = historyPairs,
                    userMessage = userMessage,
                )
            } catch (e: Exception) {
                logger.error(e) { "Chat generation failed for session=$sessionId" }
                return ChatMessageResponse(
                    sessionId = sessionId,
                    response = properties.fallbackMessage,
                    citations = emptyList(),
                    responseTimeMs = System.currentTimeMillis() - startTime,
                    timestamp = Instant.now().toString(),
                )
            }

        // Step 7: Output validation (NEVER skip this per AGENTS.md)
        val validationResult = outputValidatorService.validate(chatResult.content, sessionId.toString())

        val finalResponse =
            if (validationResult.passed) {
                chatResult.content
            } else {
                logger.warn { "Output validation failed for session=$sessionId: ${validationResult.reason}" }
                properties.fallbackMessage
            }

        // Step 8: Persist messages
        sessionService.saveMessage(sessionId, "user", userMessage)
        sessionService.saveMessage(sessionId, "assistant", finalResponse)

        // Step 9: Build response with citations
        val sources =
            assembledContext.citations.map {
                SourceInfo(source = it.source, type = it.type)
            }

        return ChatMessageResponse(
            sessionId = sessionId,
            response = finalResponse,
            citations = sources,
            responseTimeMs = System.currentTimeMillis() - startTime,
            timestamp = Instant.now().toString(),
        )
    }
}

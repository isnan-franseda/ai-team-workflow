package com.kreditpintar.chatbot.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.kreditpintar.chatbot.config.ChatClient
import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.KpSystemPrompt
import com.kreditpintar.chatbot.pipeline.ContextAssembler
import com.kreditpintar.chatbot.pipeline.VectorSearchResult
import com.kreditpintar.chatbot.pipeline.VectorSearchService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

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
    private val chatClient: ChatClient,
    private val vectorSearchService: VectorSearchService,
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

        // Step 1: Fetch history first (needed to build conversation-aware search query)
        val history = sessionService.getConversationHistory(sessionId)
        val historyPairs = history.map { Pair(it.role.lowercase(), it.content) }

        // Step 2: Build augmented search query and run vector search
        val searchQuery = buildSearchQuery(userMessage, historyPairs)
        val docResults: List<VectorSearchResult> =
            try {
                vectorSearchService.search(searchQuery)
            } catch (e: Exception) {
                logger.error(e) { "Vector search failed for session=$sessionId" }
                emptyList()
            }

        // Step 3: Assemble context from document results
        val assembledContext = contextAssembler.assemble(docResults)

        // Step 4: Generate response via ChatClient
        val chatResult =
            try {
                chatClient.chat(
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

        // Step 5: Output validation
        val validationResult = outputValidatorService.validate(chatResult.content, sessionId.toString())

        val finalResponse =
            if (validationResult.passed) {
                chatResult.content
            } else {
                logger.warn { "Output validation failed for session=$sessionId: ${validationResult.reason}" }
                properties.fallbackMessage
            }

        // Step 6: Persist messages
        sessionService.saveMessage(sessionId, "user", userMessage)
        sessionService.saveMessage(sessionId, "assistant", finalResponse)

        // Step 7: Build response with citations
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

    private fun buildSearchQuery(
        userMessage: String,
        history: List<Pair<String, String>>,
    ): String {
        if (history.isEmpty()) return userMessage
        val recentContext = history.takeLast(2).joinToString(" ") { it.second }.take(200)
        return "$recentContext $userMessage".take(500)
    }
}

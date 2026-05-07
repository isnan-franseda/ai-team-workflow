package com.kreditpintar.chatbot.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.kreditpintar.chatbot.config.RateLimitConfig
import com.kreditpintar.chatbot.service.ChatService
import com.kreditpintar.chatbot.service.SessionService
import io.github.bucket4j.ConsumptionProbe
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class CreateSessionResponse(
    @JsonProperty("session_id") val sessionId: UUID,
    @JsonProperty("created_at") val createdAt: String = java.time.Instant.now().toString(),
)

data class ChatMessageRequest(
    @JsonProperty("session_id") val sessionId: UUID,
    @JsonProperty("message") val message: String,
    @JsonProperty("language") val language: String = "id",
)

data class ChatHistoryMessage(
    @JsonProperty("role") val role: String,
    @JsonProperty("content") val content: String,
    @JsonProperty("created_at") val createdAt: String,
)

data class ChatHistoryResponse(
    @JsonProperty("session_id") val sessionId: UUID,
    @JsonProperty("messages") val messages: List<ChatHistoryMessage>,
)

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
    private val sessionService: SessionService,
    private val rateLimitConfig: RateLimitConfig,
) {
    @PostMapping("/session")
    fun createSession(): ResponseEntity<CreateSessionResponse> {
        val session = sessionService.createSession()
        return ResponseEntity.ok(CreateSessionResponse(sessionId = session.id))
    }

    @PostMapping("/send")
    fun sendMessage(
        @RequestBody request: ChatMessageRequest,
    ): ResponseEntity<Any> {
        val session =
            sessionService.getSession(request.sessionId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Session not found: ${request.sessionId}"))

        if (request.message.isBlank()) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Message cannot be empty"))
        }

        val bucket = rateLimitConfig.resolveBucket(request.sessionId.toString())
        val probe: ConsumptionProbe = bucket.tryConsumeAndReturnRemaining(1)

        if (!probe.isConsumed) {
            val waitForRefill = probe.nanosToWaitForRefill / 1_000_000_000
            logger.warn { "Rate limit exceeded for session=${request.sessionId}, wait ${waitForRefill}s" }
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-Rate-Limit-Retry-After-Seconds", waitForRefill.toString())
                .body(mapOf("error" to "Rate limit exceeded. Try again in $waitForRefill seconds."))
        }

        val response = chatService.chat(request.sessionId, request.message)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/history/{sessionId}")
    fun getHistory(
        @PathVariable sessionId: UUID,
    ): ResponseEntity<Any> {
        val session =
            sessionService.getSession(sessionId)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "Session not found: $sessionId"))

        val messages = sessionService.getConversationHistory(sessionId)
        val historyMessages =
            messages.map { msg ->
                ChatHistoryMessage(
                    role = msg.role,
                    content = msg.content,
                    createdAt = msg.createdAt.toString(),
                )
            }

        return ResponseEntity.ok(
            ChatHistoryResponse(
                sessionId = sessionId,
                messages = historyMessages,
            ),
        )
    }
}

package com.kreditpintar.chatbot.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.RateLimitConfig
import com.kreditpintar.chatbot.domain.Session
import com.kreditpintar.chatbot.service.ChatMessageResponse
import com.kreditpintar.chatbot.service.ChatService
import com.kreditpintar.chatbot.service.SessionService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import java.util.UUID

class ChatControllerTest {
    private lateinit var chatService: ChatService
    private lateinit var sessionService: SessionService
    private lateinit var properties: ChatbotProperties
    private lateinit var rateLimitConfig: RateLimitConfig
    private lateinit var chatController: ChatController
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        chatService = mock()
        sessionService = mock()
        properties =
            ChatbotProperties().apply {
                rateLimit.requestsPerMinute = 20
            }
        rateLimitConfig = RateLimitConfig(properties)
        chatController = ChatController(chatService, sessionService, rateLimitConfig)
    }

    @Test
    fun `createSession returns session ID`() {
        val sessionId = UUID.randomUUID()
        val session = Session(id = sessionId)
        whenever(sessionService.createSession()).thenReturn(session)

        val result = chatController.createSession()

        assertEquals(200, result.statusCode.value())
        val body = result.body as CreateSessionResponse
        assertEquals(sessionId, body.sessionId)
    }

    @Test
    fun `sendMessage returns 404 for non-existent session`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionService.getSession(sessionId)).thenReturn(null)

        val result = chatController.sendMessage(ChatMessageRequest(sessionId, "test"))

        assertEquals(404, result.statusCode.value())
    }

    @Test
    fun `sendMessage returns 400 for empty message`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionService.getSession(sessionId)).thenReturn(Session(id = sessionId))

        val result = chatController.sendMessage(ChatMessageRequest(sessionId, ""))

        assertEquals(400, result.statusCode.value())
    }

    @Test
    fun `sendMessage returns response for valid request`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionService.getSession(sessionId)).thenReturn(Session(id = sessionId))
        whenever(chatService.chat(sessionId, "Hello")).thenReturn(
            ChatMessageResponse(
                sessionId = sessionId,
                response = "Halo! Ada yang bisa saya bantu?",
                citations = emptyList(),
                responseTimeMs = 100L,
                timestamp = "2026-01-01T00:00:00Z",
            ),
        )

        val result = chatController.sendMessage(ChatMessageRequest(sessionId, "Hello"))

        assertEquals(200, result.statusCode.value())
        val body = result.body as ChatMessageResponse
        assertEquals("Halo! Ada yang bisa saya bantu?", body.response)
    }

    @Test
    fun `getHistory returns 404 for non-existent session`() {
        val sessionId = UUID.randomUUID()
        whenever(sessionService.getSession(sessionId)).thenReturn(null)

        val result = chatController.getHistory(sessionId)

        assertEquals(404, result.statusCode.value())
    }

    @Test
    fun `ChatController is mapped to api v1 chat`() {
        val annotation = ChatController::class.java.getAnnotation(RequestMapping::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.value.contains("/api/v1/chat"))
    }

    @Test
    fun `sendMessage is mapped to send`() {
        val method = ChatController::class.java.getMethod("sendMessage", ChatMessageRequest::class.java)
        val annotation = method.getAnnotation(PostMapping::class.java)
        assertNotNull(annotation)
        assertTrue(annotation.value.contains("/send"))
    }
}

package com.kreditpintar.chatbot.api

import com.kreditpintar.chatbot.config.RateLimitConfig
import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.service.ChatService
import com.kreditpintar.chatbot.service.ChatMessageResponse
import com.kreditpintar.chatbot.service.SessionService
import com.kreditpintar.chatbot.domain.Session
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
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
        properties = ChatbotProperties().apply {
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
                response = "Halo! Ada yang bisa saya bantu?",
                sources = emptyList(),
                sessionId = sessionId
            )
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
}

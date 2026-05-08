package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.config.ChatClient
import com.kreditpintar.chatbot.config.ChatResult
import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.domain.Message
import com.kreditpintar.chatbot.pipeline.AssembledContext
import com.kreditpintar.chatbot.pipeline.ContextAssembler
import com.kreditpintar.chatbot.pipeline.SourceCitation
import com.kreditpintar.chatbot.pipeline.VectorSearchResult
import com.kreditpintar.chatbot.pipeline.VectorSearchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import java.util.UUID

class ChatServiceTest {
    private lateinit var chatClient: ChatClient
    private lateinit var vectorSearchService: VectorSearchService
    private lateinit var contextAssembler: ContextAssembler
    private lateinit var outputValidatorService: OutputValidatorService
    private lateinit var sessionService: SessionService
    private lateinit var properties: ChatbotProperties
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        chatClient = mock()
        vectorSearchService = mock()
        contextAssembler = mock()
        outputValidatorService = mock()
        sessionService = mock()
        properties =
            ChatbotProperties().apply {
                fallbackMessage = "Maaf, saya tidak dapat menjawab."
            }
        chatService =
            ChatService(
                chatClient, vectorSearchService, contextAssembler,
                outputValidatorService, sessionService, properties,
            )
    }

    @Test
    fun `chat returns response with sources when validation passes`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Berapa limit pinjaman maksimal?"

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(
            listOf(
                VectorSearchResult(
                    chunkId = UUID.randomUUID(),
                    docId = UUID.randomUUID(),
                    chunkText = "Limit pinjaman maksimal Rp 20.000.000.",
                    source = "faq.pdf",
                    docType = "FAQ",
                    similarity = 0.92,
                ),
            ),
        )
        whenever(contextAssembler.assemble(any())).thenReturn(
            AssembledContext(
                context = "Document context",
                citations = listOf(SourceCitation("faq.pdf", "FAQ", "document")),
            ),
        )
        whenever(chatClient.chat(any(), any(), any(), any(), anyOrNull())).thenReturn(
            ChatResult(content = "Limit pinjaman maksimal adalah Rp 20.000.000."),
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = true, reason = null),
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "user", content = userMessage),
        )

        val result = chatService.chat(sessionId, userMessage)

        assertFalse(result.response.isEmpty())
        assertTrue(result.citations.isNotEmpty())
        assertEquals(sessionId, result.sessionId)
    }

    @Test
    fun `chat returns fallback when output validation fails`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Saya ingin pinjaman pasti disetujui."

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any())).thenReturn(
            AssembledContext(context = "", citations = emptyList()),
        )
        whenever(chatClient.chat(any(), any(), any(), any(), anyOrNull())).thenReturn(
            ChatResult(content = "Pinjaman Anda pasti disetujui!"),
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = false, reason = "Menjanjikan persetujuan pinjaman"),
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "assistant", content = properties.fallbackMessage),
        )

        val result = chatService.chat(sessionId, userMessage)

        assertEquals(properties.fallbackMessage, result.response)
    }

    @Test
    fun `chat uses conversation-aware query when history exists`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "berapa biayanya?"

        val history =
            listOf(
                Message(sessionId = sessionId, role = "user", content = "saya ingin tahu tentang limit pinjaman"),
                Message(sessionId = sessionId, role = "assistant", content = "Limit pinjaman maksimal Rp 20.000.000"),
            )

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(history)
        whenever(vectorSearchService.search(any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any())).thenReturn(
            AssembledContext(context = "", citations = emptyList()),
        )
        whenever(chatClient.chat(any(), any(), any(), any(), anyOrNull())).thenReturn(
            ChatResult(content = "Biaya administrasi adalah 1%."),
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = true, reason = null),
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "user", content = userMessage),
        )

        val captor = org.mockito.kotlin.argumentCaptor<String>()
        chatService.chat(sessionId, userMessage)

        org.mockito.kotlin.verify(vectorSearchService).search(captor.capture())
        assertTrue(captor.firstValue.contains("berapa biayanya?"))
        assertTrue(captor.firstValue.length > userMessage.length)
    }

    @Test
    fun `chat returns fallback when LLM call fails`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Test message"

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any())).thenReturn(
            AssembledContext(context = "", citations = emptyList()),
        )
        whenever(chatClient.chat(any(), any(), any(), any(), anyOrNull())).thenThrow(
            RuntimeException("API error"),
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "user", content = userMessage),
        )

        val result = chatService.chat(sessionId, userMessage)

        assertEquals(properties.fallbackMessage, result.response)
    }
}

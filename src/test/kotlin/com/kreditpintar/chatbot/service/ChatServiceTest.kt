package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.KpSystemPrompt
import com.kreditpintar.chatbot.config.MiniMaxClient
import com.kreditpintar.chatbot.config.ChatResult
import com.kreditpintar.chatbot.pipeline.AssembledContext
import com.kreditpintar.chatbot.pipeline.ContextAssembler
import com.kreditpintar.chatbot.pipeline.FilteredWebResult
import com.kreditpintar.chatbot.pipeline.SourceCitation
import com.kreditpintar.chatbot.pipeline.ValuesFilterService
import com.kreditpintar.chatbot.pipeline.VectorSearchResult
import com.kreditpintar.chatbot.pipeline.VectorSearchService
import com.kreditpintar.chatbot.pipeline.WebSearchEntry
import com.kreditpintar.chatbot.pipeline.WebSearchService
import com.kreditpintar.chatbot.domain.Message
import com.kreditpintar.chatbot.domain.Session
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class ChatServiceTest {

    private lateinit var miniMaxClient: MiniMaxClient
    private lateinit var vectorSearchService: VectorSearchService
    private lateinit var webSearchService: WebSearchService
    private lateinit var valuesFilterService: ValuesFilterService
    private lateinit var contextAssembler: ContextAssembler
    private lateinit var outputValidatorService: OutputValidatorService
    private lateinit var sessionService: SessionService
    private lateinit var properties: ChatbotProperties
    private lateinit var chatService: ChatService

    @BeforeEach
    fun setUp() {
        miniMaxClient = mock()
        vectorSearchService = mock()
        webSearchService = mock()
        valuesFilterService = mock()
        contextAssembler = mock()
        outputValidatorService = mock()
        sessionService = mock()
        properties = ChatbotProperties().apply {
            fallbackMessage = "Maaf, saya tidak dapat menjawab."
        }
        chatService = ChatService(
            miniMaxClient, vectorSearchService, webSearchService,
            valuesFilterService, contextAssembler, outputValidatorService,
            sessionService, properties
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
                    similarity = 0.92
                )
            )
        )
        whenever(webSearchService.search(any(), any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any(), any())).thenReturn(
            AssembledContext(
                context = "Document context",
                citations = listOf(SourceCitation("faq.pdf", "FAQ", "document"))
            )
        )
        whenever(miniMaxClient.chat(any(), any(), any(), any())).thenReturn(
            ChatResult(content = "Limit pinjaman maksimal adalah Rp 20.000.000.")
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = true, reason = null)
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "user", content = userMessage)
        )

        val result = chatService.chat(sessionId, userMessage)

        assertFalse(result.response.isEmpty())
        assertTrue(result.sources.isNotEmpty())
        assertEquals(sessionId, result.sessionId)
    }

    @Test
    fun `chat returns fallback when output validation fails`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Saya ingin pinjaman pasti disetujui."

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(emptyList())
        whenever(webSearchService.search(any(), any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any(), any())).thenReturn(
            AssembledContext(context = "", citations = emptyList())
        )
        whenever(miniMaxClient.chat(any(), any(), any(), any())).thenReturn(
            ChatResult(content = "Pinjaman Anda pasti disetujui!")
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = false, reason = "Menjanjikan persetujuan pinjaman")
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "assistant", content = properties.fallbackMessage)
        )

        val result = chatService.chat(sessionId, userMessage)

        assertEquals(properties.fallbackMessage, result.response)
    }

    @Test
    fun `chat returns fallback when MiniMax call fails`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Test message"

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(emptyList())
        whenever(webSearchService.search(any(), any())).thenReturn(emptyList())
        whenever(contextAssembler.assemble(any(), any())).thenReturn(
            AssembledContext(context = "", citations = emptyList())
        )
        whenever(miniMaxClient.chat(any(), any(), any(), any())).thenThrow(
            RuntimeException("API error")
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "user", content = userMessage)
        )

        val result = chatService.chat(sessionId, userMessage)

        assertEquals(properties.fallbackMessage, result.response)
    }

    @Test
    fun `chat triggers web search when doc results are sparse`() {
        val sessionId = UUID.randomUUID()
        val userMessage = "Apa regulasi OJK terbaru?"

        whenever(sessionService.getConversationHistory(sessionId)).thenReturn(emptyList())
        whenever(vectorSearchService.search(any())).thenReturn(emptyList()) // Sparse docs
        whenever(webSearchService.search(any(), any())).thenReturn(
            listOf(WebSearchEntry("OJK Regulasi", "New regulation", "https://ojk.go.id"))
        )
        whenever(valuesFilterService.filter(any(), any())).thenReturn(
            listOf(FilteredWebResult("OJK Regulasi", "New regulation", "https://ojk.go.id", true, null))
        )
        whenever(contextAssembler.assemble(any(), any())).thenReturn(
            AssembledContext(context = "Web context", citations = listOf(SourceCitation("https://ojk.go.id", "web", "web")))
        )
        whenever(miniMaxClient.chat(any(), any(), any(), any())).thenReturn(
            ChatResult(content = "Regulasi OJK terbaru adalah...")
        )
        whenever(outputValidatorService.validate(any(), any())).thenReturn(
            ValidationResult(passed = true, reason = null)
        )
        whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
            Message(sessionId = sessionId, role = "assistant", content = "response")
        )

        val result = chatService.chat(sessionId, userMessage)

        // Should include web source
        assertTrue(result.sources.any { it.type == "web" })
    }
}

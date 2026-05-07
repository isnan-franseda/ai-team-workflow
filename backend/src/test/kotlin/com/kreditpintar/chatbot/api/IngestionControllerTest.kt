package com.kreditpintar.chatbot.api

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.domain.ChunkRepository
import com.kreditpintar.chatbot.domain.Document
import com.kreditpintar.chatbot.domain.DocumentRepository
import com.kreditpintar.chatbot.ingestion.DocumentParser
import com.kreditpintar.chatbot.ingestion.EmbeddingService
import com.kreditpintar.chatbot.ingestion.TextChunker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant
import java.util.UUID

class IngestionControllerTest {
    private lateinit var documentRepository: DocumentRepository
    private lateinit var chunkRepository: ChunkRepository
    private lateinit var controller: IngestionController
    private lateinit var properties: ChatbotProperties

    @BeforeEach
    fun setUp() {
        documentRepository = mock()
        chunkRepository = mock()
        properties = ChatbotProperties().apply { adminKey = "test-key" }
        controller =
            IngestionController(
                documentParser = mock<DocumentParser>(),
                textChunker = mock<TextChunker>(),
                embeddingService = mock<EmbeddingService>(),
                properties = properties,
                documentRepository = documentRepository,
                chunkRepository = chunkRepository,
            )
    }

    @Test
    fun `listDocuments returns 401 for invalid key`() {
        val result = controller.listDocuments(50, null, "bad-key")
        assertEquals(401, result.statusCode.value())
    }

    @Test
    fun `listDocuments returns document list`() {
        val docId = UUID.randomUUID()
        val doc = Document(id = docId, source = "test.pdf", docType = "FAQ", fileHash = "abc", createdAt = Instant.now())
        whenever(documentRepository.findAll(any<Pageable>())).thenReturn(PageImpl(listOf(doc)))
        whenever(chunkRepository.countByDocId(docId)).thenReturn(5)

        val result = controller.listDocuments(50, null, "test-key")

        assertEquals(200, result.statusCode.value())
        val body = result.body as DocumentListResponse
        assertEquals(1, body.documents.size)
        assertEquals(5, body.documents[0].chunkCount)
        assertEquals("FAQ", body.documents[0].docType)
    }

    @Test
    fun `listDocuments filters by doc_type`() {
        whenever(documentRepository.findByDocType(any(), any())).thenReturn(PageImpl(emptyList()))

        val result = controller.listDocuments(50, "FAQ", "test-key")

        assertEquals(200, result.statusCode.value())
        val body = result.body as DocumentListResponse
        assertEquals(0L, body.total)
    }
}

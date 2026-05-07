package com.kreditpintar.chatbot.ingestion

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.EmbeddingResult
import com.kreditpintar.chatbot.config.MiniMaxClient
import com.kreditpintar.chatbot.domain.ChunkRepository
import com.kreditpintar.chatbot.domain.Document
import com.kreditpintar.chatbot.domain.DocumentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class EmbeddingServiceTest {
    private lateinit var miniMaxClient: MiniMaxClient
    private lateinit var documentRepository: DocumentRepository
    private lateinit var chunkRepository: ChunkRepository
    private lateinit var properties: ChatbotProperties
    private lateinit var embeddingService: EmbeddingService

    @BeforeEach
    fun setUp() {
        miniMaxClient = mock()
        documentRepository = mock()
        chunkRepository = mock()
        properties =
            ChatbotProperties().apply {
                ingestion.batchSize = 10
            }
        embeddingService = EmbeddingService(miniMaxClient, documentRepository, chunkRepository, properties)
    }

    @Test
    fun `ingest skips when file hash already exists`() {
        val existingDoc =
            Document(
                id = UUID.randomUUID(),
                source = "existing.txt",
                docType = "FAQ",
                fileHash = "existinghash",
            )
        whenever(documentRepository.existsByFileHash("existinghash")).thenReturn(true)
        whenever(documentRepository.findByFileHash("existinghash")).thenReturn(Optional.of(existingDoc))

        val chunks = listOf(TextChunk(text = "Test chunk", index = 0, tokenCount = 2))
        val result = embeddingService.ingestDocument("existing.txt", "FAQ", "existinghash", chunks)

        assertTrue(result.skipped)
        assertEquals(0, result.chunksCreated)
    }

    @Test
    fun `ingest creates document and embeds chunks`() {
        whenever(documentRepository.existsByFileHash("newhash")).thenReturn(false)
        whenever(documentRepository.save(any())).thenAnswer { it.arguments[0] as Document }
        whenever(chunkRepository.save(any())).thenAnswer { it.arguments[0] }

        val embedding = List(1536) { 0.1f }
        whenever(miniMaxClient.embed(any())).thenReturn(
            listOf(EmbeddingResult(embedding = embedding, tokensUsed = 100)),
        )

        val chunks =
            listOf(
                TextChunk(text = "First chunk of text.", index = 0, tokenCount = 5),
                TextChunk(text = "Second chunk of text.", index = 1, tokenCount = 5),
            )

        val result = embeddingService.ingestDocument("new.txt", "FAQ", "newhash", chunks)

        assertFalse(result.skipped)
        assertEquals(2, result.chunksCreated)
        verify(miniMaxClient).embed(any())
    }

    @Test
    fun `computeFileHash produces consistent SHA-256`() {
        val bytes = "test content".toByteArray()
        val hash1 = EmbeddingService.computeFileHash(bytes)
        val hash2 = EmbeddingService.computeFileHash(bytes)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length) // SHA-256 hex = 64 chars
    }

    @Test
    fun `computeFileHash produces different hashes for different content`() {
        val hash1 = EmbeddingService.computeFileHash("content1".toByteArray())
        val hash2 = EmbeddingService.computeFileHash("content2".toByteArray())

        assertTrue(hash1 != hash2)
    }
}

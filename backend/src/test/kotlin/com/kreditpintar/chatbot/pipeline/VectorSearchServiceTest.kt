package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.EmbeddingClient
import com.kreditpintar.chatbot.config.EmbeddingResult
import com.kreditpintar.chatbot.domain.ChunkRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class VectorSearchServiceTest {
    private lateinit var embeddingClient: EmbeddingClient
    private lateinit var chunkRepository: ChunkRepository
    private lateinit var properties: ChatbotProperties
    private lateinit var vectorSearchService: VectorSearchService

    private val docId = UUID.randomUUID()
    private val chunkId = UUID.randomUUID()
    private val adjChunkId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        embeddingClient = mock()
        chunkRepository = mock()
        properties =
            ChatbotProperties().apply {
                search.topK = 5
                search.similarityThreshold = 0.5
            }
        vectorSearchService = VectorSearchService(embeddingClient, chunkRepository, properties)

        whenever(embeddingClient.embed(any())).thenReturn(
            listOf(EmbeddingResult(embedding = List(1024) { 0.1f }, tokensUsed = 10)),
        )
    }

    @Test
    fun `search returns results above threshold with chunkIndex`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Chunk text", 2, "doc.pdf", "FAQ", 0.85),
            ),
        )
        whenever(chunkRepository.findAdjacentChunks(any(), any(), any())).thenReturn(emptyList())

        val results = vectorSearchService.search("test query")

        assertEquals(1, results.size)
        assertEquals(2, results[0].chunkIndex)
        assertEquals(0.85, results[0].similarity, 0.001)
    }

    @Test
    fun `search expands adjacent chunks for high-confidence results`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Main chunk text", 2, "doc.pdf", "FAQ", 0.85),
            ),
        )
        whenever(chunkRepository.findAdjacentChunks(eq(docId), eq(1), eq(3))).thenReturn(
            listOf(
                arrayOf(adjChunkId, docId, "Adjacent chunk text", 1, "doc.pdf", "FAQ"),
            ),
        )

        val results = vectorSearchService.search("test query")

        assertEquals(2, results.size)
        assertTrue(results.any { it.chunkId == chunkId })
        assertTrue(results.any { it.chunkId == adjChunkId })
    }

    @Test
    fun `search does not duplicate chunks already in results`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Chunk 2", 2, "doc.pdf", "FAQ", 0.85),
                arrayOf(adjChunkId, docId, "Chunk 1", 1, "doc.pdf", "FAQ", 0.72),
            ),
        )
        whenever(chunkRepository.findAdjacentChunks(any(), any(), any())).thenReturn(
            listOf(
                arrayOf(adjChunkId, docId, "Chunk 1", 1, "doc.pdf", "FAQ"),
            ),
        )

        val results = vectorSearchService.search("test query")

        assertEquals(2, results.size)
        assertEquals(1, results.count { it.chunkId == adjChunkId })
    }

    @Test
    fun `search does not expand adjacent for low-confidence results`() {
        val lowConfidenceId = UUID.randomUUID()
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(lowConfidenceId, docId, "Low conf chunk", 2, "doc.pdf", "FAQ", 0.55),
            ),
        )

        val results = vectorSearchService.search("test query")

        assertEquals(1, results.size)
        verify(chunkRepository, never()).findAdjacentChunks(any(), any(), any())
    }
}

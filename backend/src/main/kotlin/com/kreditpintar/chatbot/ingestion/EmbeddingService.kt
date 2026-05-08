package com.kreditpintar.chatbot.ingestion

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.EmbeddingClient
import com.kreditpintar.chatbot.domain.Chunk
import com.kreditpintar.chatbot.domain.ChunkRepository
import com.kreditpintar.chatbot.domain.Document
import com.kreditpintar.chatbot.domain.DocumentRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class IngestionResult(
    val documentId: UUID,
    val chunksCreated: Int,
    val tokensUsed: Int,
    val skipped: Boolean,
    val durationMs: Long,
)

@Service
class EmbeddingService(
    private val embeddingClient: EmbeddingClient,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
    private val properties: ChatbotProperties,
) {
    @Transactional
    fun ingestDocument(
        source: String,
        docType: String,
        fileHash: String,
        chunks: List<TextChunk>,
    ): IngestionResult {
        val startTime = System.currentTimeMillis()

        if (documentRepository.existsByFileHash(fileHash)) {
            val existingDoc = documentRepository.findByFileHash(fileHash).orElse(null)
            logger.info { "Document '$source' already ingested (id=${existingDoc?.id}), skipping" }
            return IngestionResult(
                documentId = existingDoc?.id ?: UUID.randomUUID(),
                chunksCreated = 0,
                tokensUsed = 0,
                skipped = true,
                durationMs = System.currentTimeMillis() - startTime,
            )
        }

        val document =
            Document(
                source = source,
                docType = docType,
                fileHash = fileHash,
            )
        documentRepository.save(document)

        var totalTokensUsed = 0
        val batchSize = properties.ingestion.batchSize

        for (batchStart in chunks.indices step batchSize) {
            val batch = chunks.subList(batchStart, minOf(batchStart + batchSize, chunks.size))
            val texts = batch.map { it.text }

            val embeddingResults = embeddingClient.embed(texts)
            totalTokensUsed += embeddingResults.sumOf { it.tokensUsed }

            for (i in batch.indices) {
                val textChunk = batch[i]
                val embeddingResult = embeddingResults[i]

                val chunkEntity =
                    Chunk(
                        docId = document.id,
                        chunkText = textChunk.text,
                        chunkIndex = textChunk.index,
                        tokenCount = textChunk.tokenCount,
                        embedding = com.pgvector.PGvector(embeddingResult.embedding.toFloatArray()),
                    )
                chunkRepository.save(chunkEntity)
            }
        }

        val durationMs = System.currentTimeMillis() - startTime
        logger.info { "Ingested document '$source': ${chunks.size} chunks, $totalTokensUsed tokens, ${durationMs}ms" }

        return IngestionResult(
            documentId = document.id,
            chunksCreated = chunks.size,
            tokensUsed = totalTokensUsed,
            skipped = false,
            durationMs = durationMs,
        )
    }

    companion object {
        fun computeFileHash(bytes: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(bytes).joinToString("") { "%02x".format(it) }
        }
    }
}

package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.EmbeddingClient
import com.kreditpintar.chatbot.domain.ChunkRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class VectorSearchResult(
    val chunkId: UUID,
    val docId: UUID,
    val chunkText: String,
    val chunkIndex: Int = 0,
    val source: String,
    val docType: String,
    val similarity: Double,
)

@Service
class VectorSearchService(
    private val embeddingClient: EmbeddingClient,
    private val chunkRepository: ChunkRepository,
    private val properties: ChatbotProperties,
) {
    fun search(query: String): List<VectorSearchResult> {
        val queryEmbedding =
            embeddingClient.embed(listOf(query)).firstOrNull()
                ?: run {
                    logger.warn { "Failed to embed query: $query" }
                    return emptyList()
                }

        val queryVectorStr = queryEmbedding.embedding.joinToString(",", "[", "]")
        val results =
            chunkRepository.findTopKBySimilarity(
                queryVector = queryVectorStr,
                limit = properties.search.topK,
            )

        val filtered = results
            .map { row ->
                VectorSearchResult(
                    chunkId = row[0] as UUID,
                    docId = row[1] as UUID,
                    chunkText = row[2] as String,
                    chunkIndex = (row[3] as Number).toInt(),
                    source = row[4] as String,
                    docType = row[5] as String,
                    similarity = (row[6] as Number).toDouble(),
                )
            }
            .filter { it.similarity >= properties.search.similarityThreshold }

        val expanded = expandWithAdjacentChunks(filtered)
        logger.info { "Vector search: ${filtered.size} direct results, ${expanded.size} after adjacent expansion" }
        return expanded
    }

    private fun expandWithAdjacentChunks(results: List<VectorSearchResult>): List<VectorSearchResult> {
        val existingIds = results.map { it.chunkId }.toMutableSet()
        val adjacent = mutableListOf<VectorSearchResult>()

        results
            .filter { it.similarity >= ADJACENT_EXPANSION_THRESHOLD }
            .forEach { result ->
                chunkRepository
                    .findAdjacentChunks(
                        docId = result.docId,
                        startIndex = result.chunkIndex - 1,
                        endIndex = result.chunkIndex + 1,
                    )
                    .forEach { row ->
                        val id = row[0] as UUID
                        if (id !in existingIds) {
                            existingIds.add(id)
                            adjacent.add(
                                VectorSearchResult(
                                    chunkId = id,
                                    docId = row[1] as UUID,
                                    chunkText = row[2] as String,
                                    chunkIndex = (row[3] as Number).toInt(),
                                    source = row[4] as String,
                                    docType = row[5] as String,
                                    similarity = 0.0,
                                ),
                            )
                        }
                    }
            }

        return results + adjacent
    }

    companion object {
        private const val ADJACENT_EXPANSION_THRESHOLD = 0.6
    }
}

package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.JinaClient
import com.kreditpintar.chatbot.domain.ChunkRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val logger = KotlinLogging.logger {}

data class VectorSearchResult(
    val chunkId: UUID,
    val docId: UUID,
    val chunkText: String,
    val source: String,
    val docType: String,
    val similarity: Double,
)

@Service
class VectorSearchService(
    private val jinaClient: JinaClient,
    private val chunkRepository: ChunkRepository,
    private val properties: ChatbotProperties,
) {
    fun search(query: String): List<VectorSearchResult> {
        val queryEmbedding =
            jinaClient.embed(listOf(query)).firstOrNull()
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

        return results
            .map { row ->
                VectorSearchResult(
                    chunkId = row[0] as UUID,
                    docId = row[1] as UUID,
                    chunkText = row[2] as String,
                    source = row[4] as String,
                    docType = row[5] as String,
                    similarity = (row[6] as Number).toDouble(),
                )
            }
            .filter { it.similarity >= properties.search.similarityThreshold }
            .also { filtered ->
                logger.info { "Vector search returned ${filtered.size} results above threshold ${properties.search.similarityThreshold}" }
            }
    }
}

package com.kreditpintar.chatbot.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

data class ChunkSearchResult(
    val chunkId: UUID,
    val docId: UUID,
    val chunkText: String,
    val chunkIndex: Int,
    val source: String,
    val docType: String,
    val similarity: Double
)

@Repository
interface ChunkRepository : JpaRepository<Chunk, UUID> {

    @Query(
        nativeQuery = true,
        value = """
            SELECT c.id, c.doc_id, c.chunk_text, c.chunk_index, d.source, d.doc_type,
                   1 - (c.embedding <=> :queryVector) AS similarity
            FROM chunks c
            JOIN documents d ON c.doc_id = d.id
            WHERE c.embedding IS NOT NULL
            ORDER BY c.embedding <=> :queryVector
            LIMIT :limit
        """
    )
    fun findTopKBySimilarity(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int
    ): List<Array<Any>>

    fun countByDocId(docId: UUID): Int
    fun deleteByDocId(docId: UUID)
}

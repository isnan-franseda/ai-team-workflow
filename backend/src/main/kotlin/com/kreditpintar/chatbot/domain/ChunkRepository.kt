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
    val similarity: Double,
)

@Repository
interface ChunkRepository : JpaRepository<Chunk, UUID> {
    @Query(
        nativeQuery = true,
        value = """
            SELECT c.id, c.doc_id, c.chunk_text, c.chunk_index, d.source, d.doc_type,
                   1 - (c.embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM chunks c
            JOIN documents d ON c.doc_id = d.id
            WHERE c.embedding IS NOT NULL
            ORDER BY c.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
        """,
    )
    fun findTopKBySimilarity(
        @Param("queryVector") queryVector: String,
        @Param("limit") limit: Int,
    ): List<Array<Any>>

    @Query(
        nativeQuery = true,
        value = """
            SELECT c.id, c.doc_id, c.chunk_text, c.chunk_index, d.source, d.doc_type
            FROM chunks c
            JOIN documents d ON c.doc_id = d.id
            WHERE c.doc_id = :docId
              AND c.chunk_index BETWEEN :startIndex AND :endIndex
            ORDER BY c.chunk_index
        """,
    )
    fun findAdjacentChunks(
        @Param("docId") docId: UUID,
        @Param("startIndex") startIndex: Int,
        @Param("endIndex") endIndex: Int,
    ): List<Array<Any>>

    fun countByDocId(docId: UUID): Int

    fun deleteByDocId(docId: UUID)
}

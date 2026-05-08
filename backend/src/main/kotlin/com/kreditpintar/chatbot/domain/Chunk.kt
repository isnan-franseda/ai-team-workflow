package com.kreditpintar.chatbot.domain

import com.kreditpintar.chatbot.config.PGvectorUserType
import com.pgvector.PGvector
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.Type
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "chunks")
class Chunk(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "doc_id", nullable = false)
    val docId: UUID,
    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    val chunkText: String,
    @Column(name = "chunk_index", nullable = false)
    val chunkIndex: Int,
    @Column(name = "token_count", nullable = false)
    val tokenCount: Int = 0,
    @Type(PGvectorUserType::class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    var embedding: PGvector? = null,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

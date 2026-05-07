package com.kreditpintar.chatbot.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "documents")
class Document(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "source", nullable = false, length = 500)
    val source: String,
    @Column(name = "doc_type", nullable = false, length = 50)
    val docType: String,
    @Column(name = "file_hash", nullable = false, length = 64)
    val fileHash: String,
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

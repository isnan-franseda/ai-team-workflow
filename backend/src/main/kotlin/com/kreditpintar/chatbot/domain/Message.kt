package com.kreditpintar.chatbot.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

enum class MessageRole {
    USER, ASSISTANT
}

@Entity
@Table(name = "messages")
class Message(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "session_id", nullable = false)
    val sessionId: UUID,

    @Column(name = "role", nullable = false, length = 20)
    val role: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
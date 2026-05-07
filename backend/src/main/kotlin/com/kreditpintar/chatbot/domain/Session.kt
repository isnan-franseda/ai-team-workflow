package com.kreditpintar.chatbot.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sessions")
class Session(
    @Id
    val id: UUID = UUID.randomUUID(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_active_at", nullable = false)
    var lastActiveAt: Instant = Instant.now()
)
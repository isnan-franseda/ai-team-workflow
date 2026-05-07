package com.kreditpintar.chatbot.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MessageRepository : JpaRepository<Message, UUID> {
    fun findBySessionIdOrderByCreatedAtAsc(sessionId: UUID): List<Message>
    fun countBySessionId(sessionId: UUID): Int
}

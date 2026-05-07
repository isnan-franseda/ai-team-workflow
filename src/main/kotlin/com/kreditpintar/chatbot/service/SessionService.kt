package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.domain.Message
import com.kreditpintar.chatbot.domain.MessageRepository
import com.kreditpintar.chatbot.domain.Session
import com.kreditpintar.chatbot.domain.SessionRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class SessionService(
    private val sessionRepository: SessionRepository,
    private val messageRepository: MessageRepository,
    private val chatbotProperties: com.kreditpintar.chatbot.config.ChatbotProperties
) {
    @Transactional
    fun createSession(): Session {
        val session = Session()
        val saved = sessionRepository.save(session)
        logger.info { "Created session: ${saved.id}" }
        return saved
    }

    fun getSession(sessionId: UUID): Session? {
        return sessionRepository.findById(sessionId).orElse(null)
    }

    @Transactional
    fun saveMessage(sessionId: UUID, role: String, content: String): Message {
        val message = Message(
            sessionId = sessionId,
            role = role,
            content = content
        )
        val saved = messageRepository.save(message)

        // Update last_active_at
        sessionRepository.findById(sessionId).ifPresent { session ->
            session.lastActiveAt = Instant.now()
            sessionRepository.save(session)
        }

        return saved
    }

    fun getConversationHistory(sessionId: UUID, limit: Int? = null): List<Message> {
        val allMessages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
        val maxMessages = limit ?: chatbotProperties.context.maxHistoryMessages
        return allMessages.takeLast(maxMessages)
    }
}

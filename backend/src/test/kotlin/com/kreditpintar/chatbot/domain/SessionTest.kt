package com.kreditpintar.chatbot.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class SessionTest {
    @Test
    fun `session has unique ID`() {
        val session1 = Session()
        val session2 = Session()

        assertNotEquals(session1.id, session2.id)
    }

    @Test
    fun `session ID is UUID`() {
        val session = Session()
        // Should not throw
        UUID.fromString(session.id.toString())
    }

    @Test
    fun `message has correct role and content`() {
        val sessionId = UUID.randomUUID()
        val message =
            Message(
                sessionId = sessionId,
                role = "user",
                content = "Test message",
            )

        assertEquals(sessionId, message.sessionId)
        assertEquals("user", message.role)
        assertEquals("Test message", message.content)
    }
}

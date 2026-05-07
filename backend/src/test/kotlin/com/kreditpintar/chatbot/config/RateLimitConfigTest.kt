package com.kreditpintar.chatbot.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RateLimitConfigTest {
    private lateinit var properties: ChatbotProperties
    private lateinit var rateLimitConfig: RateLimitConfig

    @BeforeEach
    fun setUp() {
        properties =
            ChatbotProperties().apply {
                rateLimit.requestsPerMinute = 5 // Low limit for testing
            }
        rateLimitConfig = RateLimitConfig(properties)
    }

    @Test
    fun `bucket allows requests under limit`() {
        val bucket = rateLimitConfig.resolveBucket("session-1")

        assertTrue(bucket.tryConsume(1))
    }

    @Test
    fun `bucket blocks requests over limit`() {
        val bucket = rateLimitConfig.resolveBucket("session-1")

        // Consume all 5 tokens
        repeat(5) { bucket.tryConsume(1) }

        // 6th request should be blocked
        assertFalse(bucket.tryConsume(1))
    }

    @Test
    fun `different sessions get different buckets`() {
        val bucket1 = rateLimitConfig.resolveBucket("session-1")
        val bucket2 = rateLimitConfig.resolveBucket("session-2")

        // Consume all tokens for session-1
        repeat(5) { bucket1.tryConsume(1) }

        // session-2 should still have tokens
        assertTrue(bucket2.tryConsume(1))
        assertFalse(bucket1.tryConsume(1))
    }

    @Test
    fun `same session gets same bucket`() {
        val bucket1 = rateLimitConfig.resolveBucket("session-1")
        val bucket2 = rateLimitConfig.resolveBucket("session-1")

        // Should be the same bucket instance
        assertEquals(bucket1, bucket2)
    }
}

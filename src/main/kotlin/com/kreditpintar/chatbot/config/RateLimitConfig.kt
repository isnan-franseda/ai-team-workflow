package com.kreditpintar.chatbot.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Configuration
class RateLimitConfig(
    private val properties: ChatbotProperties
) {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun resolveBucket(sessionId: String): Bucket {
        return buckets.computeIfAbsent(sessionId) { createBucket() }
    }

    private fun createBucket(): Bucket {
        val bandwidth = Bandwidth.builder()
            .capacity(properties.rateLimit.requestsPerMinute.toLong())
            .refill(Refill.greedy(properties.rateLimit.requestsPerMinute.toLong(), Duration.ofMinutes(1)))
            .build()
        return Bucket.builder()
            .addLimit(bandwidth)
            .build()
    }
}

package com.kreditpintar.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "chatbot")
class ChatbotProperties {
    var rateLimit = RateLimitProperties()
    var context = ContextProperties()
    var search = SearchProperties()
    var chunking = ChunkingProperties()
    var ingestion = IngestionProperties()
    var fallbackMessage: String =
        "Maaf, saya tidak dapat menjawab pertanyaan Anda saat ini. " +
            "Silakan hubungi layanan pelanggan Kredit Pintar untuk bantuan lebih lanjut."
    var adminKey: String = "dev-admin-key"

    class RateLimitProperties {
        var requestsPerMinute: Int = 20
    }

    class ContextProperties {
        var maxHistoryMessages: Int = 10
        var docWeightPercent: Int = 60
        var webWeightPercent: Int = 30
        var maxContextChars: Int = 3000
        var docMaxChars: Int = 1800
        var webMaxChars: Int = 900
    }

    class SearchProperties {
        var topK: Int = 5
        var similarityThreshold: Double = 0.7
        var maxWebResults: Int = 3
    }

    class ChunkingProperties {
        var chunkSize: Int = 400
        var overlap: Int = 50
    }

    class IngestionProperties {
        var batchSize: Int = 10
    }
}

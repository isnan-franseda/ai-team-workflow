package com.kreditpintar.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "llm")
class LlmProperties {
    var chat = ChatConfig()
    var embedding = EmbeddingConfig()
    var timeout = TimeoutConfig()

    class ChatConfig {
        var provider: String = "openai"
        var apiKey: String = ""
        var model: String = "gpt-4o-mini"
        var baseUrl: String = "https://api.openai.com/v1"
    }

    class EmbeddingConfig {
        var provider: String = "openai"
        var apiKey: String = ""
        var model: String = "text-embedding-3-small"
        var baseUrl: String = "https://api.openai.com/v1"
    }

    class TimeoutConfig {
        var connectSeconds: Int = 10
        var readSeconds: Int = 60
        var maxRetries: Int = 3
    }
}

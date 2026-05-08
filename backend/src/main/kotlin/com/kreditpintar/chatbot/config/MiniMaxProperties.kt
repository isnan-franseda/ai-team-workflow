package com.kreditpintar.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "minimax")
class MiniMaxProperties {
    var chat = ModelConfig()

    class ModelConfig {
        var baseUrl: String = "http://localhost:8317"
        var key: String = ""
        var model: String = "minimax-m2.7"
        var connectTimeoutSeconds: Int = 10
        var readTimeoutSeconds: Int = 60
        var maxRetries: Int = 3
    }
}

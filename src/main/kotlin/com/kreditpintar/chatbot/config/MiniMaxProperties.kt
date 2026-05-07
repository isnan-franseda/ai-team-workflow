package com.kreditpintar.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "minimax.api")
class MiniMaxProperties {
    var baseUrl: String = "https://api.minimax.chat"
    var key: String = ""
    var chatModel: String = "MiniMax-Text-01"
    var embeddingModel: String = "embo-01"
    var connectTimeoutSeconds: Int = 10
    var readTimeoutSeconds: Int = 60
    var maxRetries: Int = 3
}

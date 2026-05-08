package com.kreditpintar.chatbot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jina")
class JinaProperties {
    var baseUrl: String = "https://api.jina.ai"
    var key: String = ""
    var model: String = "jina-embeddings-v3"
    var connectTimeoutSeconds: Int = 10
    var readTimeoutSeconds: Int = 60
    var maxRetries: Int = 3
}

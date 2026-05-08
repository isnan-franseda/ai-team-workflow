package com.kreditpintar.chatbot.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@ConditionalOnProperty(name = ["llm.embedding.provider"], havingValue = "anthropic")
@Service
class AnthropicEmbeddingClient : EmbeddingClient {
    override fun embed(texts: List<String>): List<EmbeddingResult> {
        throw UnsupportedOperationException(
            "Anthropic SDK does not currently support embeddings. Use OpenAI for embeddings."
        )
    }
}

package com.kreditpintar.chatbot.config

data class EmbeddingResult(
    val embedding: List<Float>,
    val tokensUsed: Int,
)

interface EmbeddingClient {
    fun embed(texts: List<String>): List<EmbeddingResult>
}

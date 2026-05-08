package com.kreditpintar.chatbot.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.embeddings.EmbeddingCreateParams
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@ConditionalOnProperty(name = ["llm.embedding.provider"], havingValue = "openai", matchIfMissing = true)
@Service
class OpenAiEmbeddingClient(
    private val properties: LlmProperties,
) : EmbeddingClient {
    private val client: OpenAIClient by lazy {
        OpenAIOkHttpClient.builder()
            .apiKey(properties.embedding.apiKey)
            .baseUrl(properties.embedding.baseUrl)
            .build()
    }

    override fun embed(texts: List<String>): List<EmbeddingResult> {
        val params = EmbeddingCreateParams.builder()
            .model(properties.embedding.model)
            .inputOfArrayOfStrings(texts)
            .build()

        return try {
            val response = client.embeddings().create(params)
            val usageTokens = response.usage().totalTokens()
            response.data().mapIndexed { i, embedding ->
                EmbeddingResult(
                    embedding = embedding.embedding().map { it.toFloat() },
                    tokensUsed = if (i == 0) usageTokens.toInt() else 0,
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "OpenAI embedding call failed" }
            throw e
        }
    }
}

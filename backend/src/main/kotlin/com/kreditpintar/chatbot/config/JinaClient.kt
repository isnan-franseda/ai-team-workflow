package com.kreditpintar.chatbot.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

data class EmbeddingResult(
    val embedding: List<Float>,
    val tokensUsed: Int,
)

@Component
class JinaClient(
    private val properties: JinaProperties,
    private val objectMapper: ObjectMapper,
) {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(properties.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(properties.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun embed(texts: List<String>): List<EmbeddingResult> {
        val requestJson =
            objectMapper.createObjectNode().apply {
                put("model", properties.model)
                putArray("input").apply { texts.forEach { add(it) } }
            }

        val response = post("/v1/embeddings", requestJson)
        val dataNode = response.path("data")
        val usageTokens = response.path("usage").path("total_tokens").intValue()

        return texts.indices.map { i ->
            val embedding = dataNode[i].path("embedding").map { it.floatValue() }
            EmbeddingResult(
                embedding = embedding,
                tokensUsed = if (i == 0) usageTokens else 0,
            )
        }
    }

    private fun post(
        path: String,
        body: ObjectNode,
    ): JsonNode {
        var lastException: IOException? = null

        for (attempt in 1..properties.maxRetries) {
            try {
                val request =
                    Request.Builder()
                        .url("${properties.baseUrl}$path")
                        .header("Authorization", "Bearer ${properties.key}")
                        .header("Content-Type", "application/json")
                        .post(body.toString().toRequestBody(jsonMediaType))
                        .build()

                val result: JsonNode? =
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            val errorBody = response.body?.string() ?: "Unknown error"
                            logger.warn { "Jina API error (attempt $attempt): ${response.code} - $errorBody" }
                            if (attempt == properties.maxRetries) {
                                throw IOException("Jina API returned ${response.code}: $errorBody")
                            }
                            null
                        } else {
                            val responseBody =
                                response.body?.string()
                                    ?: throw IOException("Empty response body from Jina API")
                            objectMapper.readTree(responseBody)
                        }
                    }
                if (result != null) return result
            } catch (e: IOException) {
                lastException = e
                logger.warn(e) { "Jina API call failed (attempt $attempt)" }
            }
        }

        throw lastException ?: IOException("Failed to call Jina API after ${properties.maxRetries} attempts")
    }
}

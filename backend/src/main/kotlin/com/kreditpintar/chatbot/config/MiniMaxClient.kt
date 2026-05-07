package com.kreditpintar.chatbot.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
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
    val tokensUsed: Int
)

data class ChatResult(
    val content: String,
    val webSearchResults: List<WebSearchResult> = emptyList()
)

data class WebSearchResult(
    val title: String,
    val content: String,
    val url: String
)

@Component
class MiniMaxClient(
    private val properties: MiniMaxProperties,
    private val objectMapper: ObjectMapper
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(properties.connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(properties.readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun embed(texts: List<String>): List<EmbeddingResult> {
        val requestJson = objectMapper.createObjectNode().apply {
            put("model", properties.embeddingModel)
            putArray("texts").apply {
                texts.forEach { add(it) }
            }
        }

        val response = post("/v1/embeddings", requestJson)
        val dataNode = response.path("data")

        return texts.indices.map { i ->
            val embeddingNode = dataNode[i].path("embedding")
            val embedding = embeddingNode.map { it.floatValue() }
            val usageTokens = response.path("usage").path("total_tokens").intValue()
            EmbeddingResult(
                embedding = embedding,
                tokensUsed = if (i == 0) usageTokens else 0 // Count tokens once
            )
        }
    }

    fun chat(
        systemPrompt: String,
        context: String,
        history: List<Pair<String, String>>,
        userMessage: String
    ): ChatResult {
        val messages = objectMapper.createArrayNode()

        messages.addObject().apply {
            put("role", "system")
            put("content", "$systemPrompt\n\n$context")
        }

        history.forEach { (role, content) ->
            messages.addObject().apply {
                put("role", role)
                put("content", content)
            }
        }

        messages.addObject().apply {
            put("role", "user")
            put("content", userMessage)
        }

        val requestJson = objectMapper.createObjectNode().apply {
            put("model", properties.chatModel)
            set<ArrayNode>("messages", messages)
        }

        val response = post("/v1/chat/completions", requestJson)
        val content = response.path("choices")[0].path("message").path("content").asText()

        return ChatResult(content = content)
    }

    fun chatWithWebSearch(
        systemPrompt: String,
        context: String,
        history: List<Pair<String, String>>,
        userMessage: String
    ): ChatResult {
        val messages = objectMapper.createArrayNode()

        messages.addObject().apply {
            put("role", "system")
            put("content", "$systemPrompt\n\n$context")
        }

        history.forEach { (role, content) ->
            messages.addObject().apply {
                put("role", role)
                put("content", content)
            }
        }

        messages.addObject().apply {
            put("role", "user")
            put("content", userMessage)
        }

        val tools = objectMapper.createArrayNode()
        tools.addObject().apply {
            put("type", "web_search")
            put("enable", true)
        }

        val requestJson = objectMapper.createObjectNode().apply {
            put("model", properties.chatModel)
            set<ArrayNode>("messages", messages)
            set<ArrayNode>("tools", tools)
        }

        val response = post("/v1/chat/completions", requestJson)
        val choicesNode = response.path("choices")
        val messageNode = choicesNode[0].path("message")
        val content = messageNode.path("content").asText()

        val webResults = mutableListOf<WebSearchResult>()
        val webSearchNode = messageNode.path("web_search")
        if (webSearchNode.isArray) {
            webSearchNode.forEach { result ->
                webResults.add(
                    WebSearchResult(
                        title = result.path("title").asText(),
                        content = result.path("content").asText(),
                        url = result.path("url").asText()
                    )
                )
            }
        }

        return ChatResult(content = content, webSearchResults = webResults)
    }

    fun validateOutput(response: String, validationPrompt: String): String {
        val messages = objectMapper.createArrayNode()

        messages.addObject().apply {
            put("role", "system")
            put("content", validationPrompt)
        }

        messages.addObject().apply {
            put("role", "user")
            put("content", response)
        }

        val requestJson = objectMapper.createObjectNode().apply {
            put("model", properties.chatModel)
            set<ArrayNode>("messages", messages)
            put("temperature", 0.0)
        }

        val result = post("/v1/chat/completions", requestJson)
        return result.path("choices")[0].path("message").path("content").asText()
    }

    private fun post(path: String, body: ObjectNode): JsonNode {
        var lastException: IOException? = null

        for (attempt in 1..properties.maxRetries) {
            try {
                val request = Request.Builder()
                    .url("${properties.baseUrl}$path")
                    .header("Authorization", "Bearer ${properties.key}")
                    .header("Content-Type", "application/json")
                    .post(body.toString().toRequestBody(jsonMediaType))
                    .build()

                val result: JsonNode? = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "Unknown error"
                        logger.warn { "MiniMax API error (attempt $attempt): ${response.code} - $errorBody" }
                        if (attempt == properties.maxRetries) {
                            throw IOException("MiniMax API returned ${response.code}: $errorBody")
                        }
                        null
                    } else {
                        val responseBody = response.body?.string()
                            ?: throw IOException("Empty response body from MiniMax API")
                        objectMapper.readTree(responseBody)
                    }
                }
                if (result != null) return result
            } catch (e: IOException) {
                lastException = e
                logger.warn(e) { "MiniMax API call failed (attempt $attempt)" }
            }
        }

        throw lastException ?: IOException("Failed to call MiniMax API after ${properties.maxRetries} attempts")
    }
}

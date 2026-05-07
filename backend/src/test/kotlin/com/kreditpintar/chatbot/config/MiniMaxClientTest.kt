package com.kreditpintar.chatbot.config

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MiniMaxClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var miniMaxClient: MiniMaxClient
    private lateinit var properties: MiniMaxProperties
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        properties =
            MiniMaxProperties().apply {
                baseUrl = mockWebServer.url("/").toString().trimEnd('/')
                key = "test-api-key"
                chatModel = "MiniMax-Text-01"
                embeddingModel = "embo-01"
                maxRetries = 3
                connectTimeoutSeconds = 5
                readTimeoutSeconds = 10
            }
        miniMaxClient = MiniMaxClient(properties, objectMapper)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `embed returns embeddings for texts`() {
        val embeddingResponse =
            """
            {
                "data": [
                    {
                        "object": "embedding",
                        "embedding": ${(1..1536).map { 0.01 }},
                        "index": 0
                    }
                ],
                "usage": {
                    "total_tokens": 10
                }
            }
            """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(embeddingResponse)
                .setHeader("Content-Type", "application/json"),
        )

        val results = miniMaxClient.embed(listOf("test text"))

        assertEquals(1, results.size)
        assertEquals(1536, results[0].embedding.size)
        assertEquals(10, results[0].tokensUsed)
    }

    @Test
    fun `chat returns content from completion`() {
        val chatResponse =
            """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Halo! Ada yang bisa saya bantu?"
                        }
                    }
                ]
            }
            """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(chatResponse)
                .setHeader("Content-Type", "application/json"),
        )

        val result = miniMaxClient.chat("system prompt", "context", emptyList(), "Hello")

        assertEquals("Halo! Ada yang bisa saya bantu?", result.content)
    }

    @Test
    fun `chat retries on failure and succeeds`() {
        // First call fails
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        // Second call succeeds
        val chatResponse =
            """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Retried response"
                        }
                    }
                ]
            }
            """.trimIndent()
        mockWebServer.enqueue(
            MockResponse()
                .setBody(chatResponse)
                .setHeader("Content-Type", "application/json"),
        )

        val result = miniMaxClient.chat("system", "context", emptyList(), "test")

        assertEquals("Retried response", result.content)
    }

    @Test
    fun `chatWithWebSearch returns content and web results`() {
        val webSearchResponse =
            """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "Berikut informasi dari web.",
                            "web_search": [
                                {
                                    "title": "OJK Regulasi",
                                    "content": "Regulasi terbaru.",
                                    "url": "https://ojk.go.id"
                                }
                            ]
                        }
                    }
                ]
            }
            """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(webSearchResponse)
                .setHeader("Content-Type", "application/json"),
        )

        val result = miniMaxClient.chatWithWebSearch("system", "context", emptyList(), "regulasi OJK")

        assertEquals("Berikut informasi dari web.", result.content)
        assertEquals(1, result.webSearchResults.size)
        assertEquals("OJK Regulasi", result.webSearchResults[0].title)
    }

    @Test
    fun `validateOutput returns validation response`() {
        val validateResponse =
            """
            {
                "choices": [
                    {
                        "message": {
                            "role": "assistant",
                            "content": "PASS\nAlasan: Respons sesuai standar"
                        }
                    }
                ]
            }
            """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setBody(validateResponse)
                .setHeader("Content-Type", "application/json"),
        )

        val result = miniMaxClient.validateOutput("test response", "validation prompt")

        assertTrue(result.contains("PASS"))
    }
}

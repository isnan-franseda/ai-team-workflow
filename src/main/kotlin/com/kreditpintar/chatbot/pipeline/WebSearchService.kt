package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.MiniMaxClient
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

data class WebSearchEntry(
    val title: String,
    val content: String,
    val url: String
)

@Service
class WebSearchService(
    private val miniMaxClient: MiniMaxClient
) {
    fun search(
        query: String,
        history: List<Pair<String, String>> = emptyList()
    ): List<WebSearchEntry> {
        logger.info { "Performing web search for query: $query" }

        return try {
            val result = miniMaxClient.chatWithWebSearch(
                systemPrompt = "You are a search assistant. Return relevant search results for the user query.",
                context = "",
                history = history,
                userMessage = query
            )

            result.webSearchResults.map { entry ->
                WebSearchEntry(
                    title = entry.title,
                    content = entry.content,
                    url = entry.url
                )
            }.also { results ->
                logger.info { "Web search returned ${results.size} results" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Web search failed for query: $query" }
            emptyList()
        }
    }
}

package com.kreditpintar.chatbot.ingestion

import com.kreditpintar.chatbot.config.ChatbotProperties
import org.springframework.stereotype.Component

data class TextChunk(
    val text: String,
    val index: Int,
    val tokenCount: Int,
)

@Component
class TextChunker(
    private val properties: ChatbotProperties,
) {
    fun chunk(text: String): List<TextChunk> {
        val normalizedText = text
            .replace(Regex("[ \t]{2,}"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()

        val chunkSize = properties.chunking.chunkSize
        val overlap = properties.chunking.overlap

        if (normalizedText.isEmpty()) return emptyList()

        if (normalizedText.length <= chunkSize) {
            return listOf(
                TextChunk(
                    text = normalizedText,
                    index = 0,
                    tokenCount = estimateTokens(normalizedText),
                ),
            )
        }

        val chunks = mutableListOf<TextChunk>()
        var index = 0
        var position = 0

        while (position < normalizedText.length) {
            val end = findChunkEnd(normalizedText, position, chunkSize)
            val chunkText = normalizedText.substring(position, end).trim()

            if (chunkText.isNotEmpty()) {
                chunks.add(
                    TextChunk(
                        text = chunkText,
                        index = index,
                        tokenCount = estimateTokens(chunkText),
                    ),
                )
                index++
            }

            position =
                if (end >= normalizedText.length) {
                    normalizedText.length
                } else {
                    end - overlap
                }
        }

        return chunks
    }

    private fun findChunkEnd(
        text: String,
        start: Int,
        maxChunkSize: Int,
    ): Int {
        val targetEnd = start + maxChunkSize
        if (targetEnd >= text.length) return text.length

        // Try to find a sentence boundary near the target end
        var end = targetEnd
        while (end > start + maxChunkSize / 2 && end > start) {
            val char = text[end]
            if (char == '.' || char == '!' || char == '?' || char == '\n') {
                return end + 1 // Include the punctuation
            }
            end--
        }

        // Fall back to word boundary
        end = targetEnd
        while (end > start + maxChunkSize / 2 && end > start) {
            if (text[end].isWhitespace()) {
                return end
            }
            end--
        }

        // Last resort: hard split at maxChunkSize
        return targetEnd
    }

    private fun estimateTokens(text: String): Int {
        // Rough estimation: 1 token ~ 4 characters for Indonesian text
        return (text.length / 4).coerceAtLeast(1)
    }
}

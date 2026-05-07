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
        val chunkSize = properties.chunking.chunkSize
        val overlap = properties.chunking.overlap

        if (text.length <= chunkSize) {
            return listOf(
                TextChunk(
                    text = text,
                    index = 0,
                    tokenCount = estimateTokens(text),
                ),
            )
        }

        val chunks = mutableListOf<TextChunk>()
        var index = 0
        var position = 0

        while (position < text.length) {
            val end = findChunkEnd(text, position, chunkSize)
            val chunkText = text.substring(position, end).trim()

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
                if (end >= text.length) {
                    text.length
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

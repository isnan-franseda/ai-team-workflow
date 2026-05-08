package com.kreditpintar.chatbot.ingestion

import com.kreditpintar.chatbot.config.ChatbotProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TextChunkerTest {
    private lateinit var properties: ChatbotProperties
    private lateinit var textChunker: TextChunker

    @BeforeEach
    fun setUp() {
        properties =
            ChatbotProperties().apply {
                chunking.chunkSize = 400
                chunking.overlap = 50
            }
        textChunker = TextChunker(properties)
    }

    @Test
    fun `short text returns single chunk`() {
        val text = "Ini adalah teks pendek."
        val chunks = textChunker.chunk(text)

        assertEquals(1, chunks.size)
        assertEquals(text, chunks[0].text)
        assertEquals(0, chunks[0].index)
    }

    @Test
    fun `long text is split into multiple chunks`() {
        val text = "A".repeat(1000)
        val chunks = textChunker.chunk(text)

        assertTrue(chunks.size > 1)
        chunks.forEachIndexed { index, chunk ->
            assertEquals(index, chunk.index)
        }
    }

    @Test
    fun `chunks preserve content coverage`() {
        val text =
            buildString {
                repeat(20) {
                    append("Kalimat nomor $it untuk pengujian. ")
                }
            }
        val chunks = textChunker.chunk(text)

        val allChunkTexts = chunks.joinToString("") { it.text }
        // All original content should be covered (in some form)
        assertTrue(allChunkTexts.length > text.length / 2)
    }

    @Test
    fun `each chunk has estimated token count`() {
        val text = "A".repeat(500)
        val chunks = textChunker.chunk(text)

        chunks.forEach { chunk ->
            assertTrue(chunk.tokenCount > 0)
        }
    }

    @Test
    fun `empty text returns empty list`() {
        val text = ""
        val chunks = textChunker.chunk(text)

        assertTrue(chunks.isEmpty())
    }

    @Test
    fun `multiple spaces are collapsed to single space`() {
        val text = "Batas   pinjaman   adalah   Rp 20.000.000."
        val chunks = textChunker.chunk(text)

        assertEquals(1, chunks.size)
        assertFalse(chunks[0].text.contains("  "))
        assertTrue(chunks[0].text.contains("Batas pinjaman adalah"))
    }

    @Test
    fun `excessive newlines are collapsed to double newline`() {
        val text = "Paragraf pertama.\n\n\n\nParagraf kedua."
        val chunks = textChunker.chunk(text)

        assertEquals(1, chunks.size)
        assertFalse(chunks[0].text.contains("\n\n\n"))
        assertTrue(chunks[0].text.contains("Paragraf pertama"))
        assertTrue(chunks[0].text.contains("Paragraf kedua"))
    }

    @Test
    fun `chunk indices are sequential`() {
        val text = "Test sentence. " + "Another sentence. ".repeat(30)
        val chunks = textChunker.chunk(text)

        for (i in chunks.indices) {
            assertEquals(i, chunks[i].index)
        }
    }
}

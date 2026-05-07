package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ContextAssemblerTest {
    private lateinit var properties: ChatbotProperties
    private lateinit var contextAssembler: ContextAssembler

    @BeforeEach
    fun setUp() {
        properties =
            ChatbotProperties().apply {
                context.docMaxChars = 1800
                context.webMaxChars = 900
            }
        contextAssembler = ContextAssembler(properties)
    }

    @Test
    fun `assemble includes document context with citations`() {
        val docResults =
            listOf(
                VectorSearchResult(
                    chunkId = UUID.randomUUID(),
                    docId = UUID.randomUUID(),
                    chunkText = "Limit pinjaman maksimal Rp 20.000.000.",
                    source = "faq.pdf",
                    docType = "FAQ",
                    similarity = 0.9,
                ),
            )

        val result = contextAssembler.assemble(docResults, emptyList())

        assertTrue(result.context.contains("DOKUMEN KREDIT PINTAR"))
        assertTrue(result.context.contains("faq.pdf"))
        assertTrue(result.context.contains("Limit pinjaman"))
        assertEquals(1, result.citations.size)
        assertEquals("document", result.citations[0].type)
    }

    @Test
    fun `assemble includes passed web results with citations`() {
        val webResults =
            listOf(
                FilteredWebResult(
                    title = "OJK Regulasi",
                    content = "Regulasi terbaru dari OJK.",
                    url = "https://ojk.go.id",
                    passed = true,
                    filterReason = null,
                ),
            )

        val result = contextAssembler.assemble(emptyList(), webResults)

        assertTrue(result.context.contains("INFORMASI WEB"))
        assertTrue(result.context.contains("OJK Regulasi"))
        assertEquals(1, result.citations.size)
        assertEquals("web", result.citations[0].type)
    }

    @Test
    fun `assemble excludes failed web results`() {
        val webResults =
            listOf(
                FilteredWebResult(
                    title = "Bad content",
                    content = "Bad content",
                    url = "https://bad.com",
                    passed = false,
                    filterReason = "Urgency tactics",
                ),
            )

        val result = contextAssembler.assemble(emptyList(), webResults)

        assertTrue(result.citations.isEmpty())
    }

    @Test
    fun `assemble handles empty results`() {
        val result = contextAssembler.assemble(emptyList(), emptyList())

        assertTrue(result.context.contains("DOKUMEN KREDIT PINTAR"))
        assertTrue(result.citations.isEmpty())
    }

    @Test
    fun `assemble respects doc max chars limit`() {
        val props =
            ChatbotProperties().apply {
                context.docMaxChars = 50
                context.webMaxChars = 50
            }
        val assembler = ContextAssembler(props)

        val docResults =
            listOf(
                VectorSearchResult(
                    chunkId = UUID.randomUUID(),
                    docId = UUID.randomUUID(),
                    chunkText = "A".repeat(100),
                    source = "big.pdf",
                    docType = "FAQ",
                    similarity = 0.9,
                ),
            )

        val result = assembler.assemble(docResults, emptyList())

        // Context should be limited by docMaxChars
        assertTrue(result.context.length < 300) // header + limited content
    }
}

package com.kreditpintar.chatbot.ingestion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.ClassPathResource
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.StreamUtils
import java.nio.charset.StandardCharsets

class DocumentParserTest {

    private lateinit var documentParser: DocumentParser

    @BeforeEach
    fun setUp() {
        documentParser = DocumentParser()
    }

    @Test
    fun `parse txt file returns content`() {
        val content = "Ini adalah dokumen FAQ Kredit Pintar."
        val file = MockMultipartFile(
            "file",
            "faq.txt",
            "text/plain",
            content.toByteArray(StandardCharsets.UTF_8)
        )

        val result = documentParser.parse(file, "FAQ")

        assertEquals(content, result.text)
        assertEquals("FAQ", result.metadata["docType"])
    }

    @Test
    fun `parse unsupported file type throws exception`() {
        val file = MockMultipartFile(
            "file",
            "test.xlsx",
            "application/vnd.ms-excel",
            "data".toByteArray()
        )

        assertThrows<IllegalArgumentException> {
            documentParser.parse(file, "FAQ")
        }
    }

    @Test
    fun `parse preserves filename in metadata`() {
        val content = "Test content"
        val file = MockMultipartFile(
            "file",
            "terms-and-conditions.txt",
            "text/plain",
            content.toByteArray(StandardCharsets.UTF_8)
        )

        val result = documentParser.parse(file, "TOS")

        assertEquals("terms-and-conditions.txt", result.metadata["filename"])
    }

    @Test
    fun `parse includes file size in metadata`() {
        val content = "Test content"
        val file = MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            content.toByteArray(StandardCharsets.UTF_8)
        )

        val result = documentParser.parse(file, "FAQ")

        assertTrue(result.metadata["fileSizeBytes"]!!.toInt() > 0)
    }
}

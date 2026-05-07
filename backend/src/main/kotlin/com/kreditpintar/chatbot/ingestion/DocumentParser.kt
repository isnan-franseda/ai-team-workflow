package com.kreditpintar.chatbot.ingestion

import mu.KotlinLogging
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream

private val logger = KotlinLogging.logger {}

data class ParsedDocument(
    val text: String,
    val metadata: Map<String, String>,
)

@Component
class DocumentParser {
    fun parse(
        file: MultipartFile,
        docType: String,
    ): ParsedDocument {
        val bytes = file.bytes
        val filename = file.originalFilename ?: "unknown"
        val extension = filename.substringAfterLast('.').lowercase()

        val text =
            when (extension) {
                "pdf" -> parsePdf(bytes)
                "docx", "doc" -> parseDocx(bytes)
                "txt" -> String(bytes, Charsets.UTF_8)
                else -> throw IllegalArgumentException("Unsupported file type: $extension. Supported: pdf, docx, txt")
            }

        val metadata =
            mapOf(
                "filename" to filename,
                "docType" to docType,
                "fileSizeBytes" to bytes.size.toString(),
            )

        logger.info { "Parsed document '$filename': ${text.length} chars, type=$docType" }
        return ParsedDocument(text = text, metadata = metadata)
    }

    private fun parsePdf(bytes: ByteArray): String {
        return Loader.loadPDF(bytes).use { document ->
            val stripper = PDFTextStripper()
            val text = StringBuilder()
            for (pageNum in 1..document.numberOfPages) {
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val pageText = stripper.getText(document)
                if (pageText.isNotBlank()) {
                    text.append(pageText).append("\n\n")
                }
            }
            text.toString().trim()
        }
    }

    private fun parseDocx(bytes: ByteArray): String {
        ByteArrayInputStream(bytes).use { inputStream ->
            XWPFDocument(inputStream).use { document ->
                val text = StringBuilder()
                for (paragraph in document.paragraphs) {
                    val paraText = paragraph.text
                    if (paraText.isNotBlank()) {
                        text.append(paraText).append("\n")
                    }
                }
                for (table in document.tables) {
                    for (row in table.rows) {
                        for (cell in row.tableCells) {
                            val cellText = cell.text
                            if (cellText.isNotBlank()) {
                                text.append(cellText).append(" ")
                            }
                        }
                        text.append("\n")
                    }
                }
                return text.toString().trim()
            }
        }
    }
}

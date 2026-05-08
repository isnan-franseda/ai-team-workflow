package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ContextAssembler(
    private val properties: ChatbotProperties,
) {
    fun assemble(docResults: List<VectorSearchResult>): AssembledContext {
        val docMaxChars = properties.context.docMaxChars

        val docSection = StringBuilder("=== DOKUMEN KREDIT PINTAR ===\n")
        var docCharsUsed = 0

        for (result in docResults) {
            val entry = "[Sumber: ${result.source} (${result.docType})]\n${result.chunkText}\n\n"
            if (docCharsUsed + entry.length > docMaxChars) break
            docSection.append(entry)
            docCharsUsed += entry.length
        }

        val citations = docResults.map { result ->
            SourceCitation(source = result.source, docType = result.docType, type = "document")
        }

        logger.info { "Assembled context: ${docSection.length} chars, ${citations.size} citations" }

        return AssembledContext(
            context = docSection.toString(),
            citations = citations,
        )
    }
}

data class AssembledContext(
    val context: String,
    val citations: List<SourceCitation>,
)

data class SourceCitation(
    val source: String,
    val docType: String,
    val type: String,
)

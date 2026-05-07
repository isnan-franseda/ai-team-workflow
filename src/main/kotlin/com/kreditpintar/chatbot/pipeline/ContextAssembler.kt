package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class ContextAssembler(
    private val properties: ChatbotProperties
) {
    fun assemble(
        docResults: List<VectorSearchResult>,
        filteredWebResults: List<FilteredWebResult>
    ): AssembledContext {
        val docMaxChars = properties.context.docMaxChars
        val webMaxChars = properties.context.webMaxChars

        val docSection = StringBuilder("=== DOKUMEN KREDIT PINTAR ===\n")
        var docCharsUsed = 0

        for (result in docResults) {
            val entry = "[Sumber: ${result.source} (${result.docType})]\n${result.chunkText}\n\n"
            if (docCharsUsed + entry.length > docMaxChars) break
            docSection.append(entry)
            docCharsUsed += entry.length
        }

        val webSection = StringBuilder("\n=== INFORMASI WEB ===\n")
        var webCharsUsed = 0

        for (result in filteredWebResults.filter { it.passed }) {
            val entry = "[Sumber Web: ${result.title} - ${result.url}]\n${result.content}\n\n"
            if (webCharsUsed + entry.length > webMaxChars) break
            webSection.append(entry)
            webCharsUsed += entry.length
        }

        val fullContext = docSection.toString() + webSection.toString()

        val citations = mutableListOf<SourceCitation>()
        for (result in docResults) {
            citations.add(SourceCitation(source = result.source, docType = result.docType, type = "document"))
        }
        for (result in filteredWebResults.filter { it.passed }) {
            citations.add(SourceCitation(source = result.url, docType = "web", type = "web"))
        }

        logger.info { "Assembled context: ${fullContext.length} chars, ${citations.size} citations" }

        return AssembledContext(
            context = fullContext,
            citations = citations
        )
    }
}

data class AssembledContext(
    val context: String,
    val citations: List<SourceCitation>
)

data class SourceCitation(
    val source: String,
    val docType: String,
    val type: String
)

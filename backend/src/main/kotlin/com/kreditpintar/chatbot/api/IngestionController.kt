package com.kreditpintar.chatbot.api

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.ingestion.DocumentParser
import com.kreditpintar.chatbot.ingestion.EmbeddingService
import com.kreditpintar.chatbot.ingestion.IngestionResult
import com.kreditpintar.chatbot.ingestion.TextChunker
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger {}

data class IngestionResponse(
    val documentId: String,
    val chunksCreated: Int,
    val tokensUsed: Int,
    val skipped: Boolean,
    val durationMs: Long,
)

data class BatchIngestionResponse(
    val results: List<IngestionResponse>,
    val totalChunksCreated: Int,
    val totalTokensUsed: Int,
    val totalDurationMs: Long,
)

@RestController
@RequestMapping("/admin")
class IngestionController(
    private val documentParser: DocumentParser,
    private val textChunker: TextChunker,
    private val embeddingService: EmbeddingService,
    private val properties: ChatbotProperties,
) {
    @PostMapping("/ingest")
    fun ingestDocument(
        @RequestParam file: MultipartFile,
        @RequestParam docType: String,
        @RequestHeader("X-Admin-Key") adminKey: String,
    ): ResponseEntity<Any> {
        if (adminKey != properties.adminKey) {
            logger.warn { "Invalid admin key attempted for ingestion" }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid admin key"))
        }

        val validDocTypes = listOf("FAQ", "TOS", "BRAND", "HOWTO")
        if (docType !in validDocTypes) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Invalid docType. Must be one of: $validDocTypes"))
        }

        try {
            val bytes = file.bytes
            val fileHash = EmbeddingService.computeFileHash(bytes)

            val parsed = documentParser.parse(file, docType)
            val chunks = textChunker.chunk(parsed.text)

            val result =
                embeddingService.ingestDocument(
                    source = parsed.metadata["filename"] ?: "unknown",
                    docType = docType,
                    fileHash = fileHash,
                    chunks = chunks,
                )

            return ResponseEntity.ok(mapIngestionResult(result))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid input")))
        } catch (e: Exception) {
            logger.error(e) { "Document ingestion failed" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Ingestion failed: ${e.message}"))
        }
    }

    @PostMapping("/ingest/batch")
    fun ingestBatch(
        @RequestParam files: List<MultipartFile>,
        @RequestParam docType: String,
        @RequestHeader("X-Admin-Key") adminKey: String,
    ): ResponseEntity<Any> {
        if (adminKey != properties.adminKey) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid admin key"))
        }

        val validDocTypes = listOf("FAQ", "TOS", "BRAND", "HOWTO")
        if (docType !in validDocTypes) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "Invalid docType. Must be one of: $validDocTypes"))
        }

        val results = mutableListOf<IngestionResponse>()
        var totalChunks = 0
        var totalTokens = 0
        val startTime = System.currentTimeMillis()

        for (file in files) {
            try {
                val bytes = file.bytes
                val fileHash = EmbeddingService.computeFileHash(bytes)

                val parsed = documentParser.parse(file, docType)
                val chunks = textChunker.chunk(parsed.text)

                val result =
                    embeddingService.ingestDocument(
                        source = parsed.metadata["filename"] ?: "unknown",
                        docType = docType,
                        fileHash = fileHash,
                        chunks = chunks,
                    )

                results.add(mapIngestionResult(result))
                totalChunks += result.chunksCreated
                totalTokens += result.tokensUsed
            } catch (e: Exception) {
                logger.error(e) { "Batch ingestion failed for file: ${file.originalFilename}" }
                results.add(
                    IngestionResponse(
                        documentId = "error",
                        chunksCreated = 0,
                        tokensUsed = 0,
                        skipped = false,
                        durationMs = 0,
                    ),
                )
            }
        }

        return ResponseEntity.ok(
            BatchIngestionResponse(
                results = results,
                totalChunksCreated = totalChunks,
                totalTokensUsed = totalTokens,
                totalDurationMs = System.currentTimeMillis() - startTime,
            ),
        )
    }

    private fun mapIngestionResult(result: IngestionResult): IngestionResponse {
        return IngestionResponse(
            documentId = result.documentId.toString(),
            chunksCreated = result.chunksCreated,
            tokensUsed = result.tokensUsed,
            skipped = result.skipped,
            durationMs = result.durationMs,
        )
    }
}

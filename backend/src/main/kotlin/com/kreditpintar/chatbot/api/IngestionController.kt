package com.kreditpintar.chatbot.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.domain.ChunkRepository
import com.kreditpintar.chatbot.domain.DocumentRepository
import com.kreditpintar.chatbot.ingestion.DocumentParser
import com.kreditpintar.chatbot.ingestion.EmbeddingService
import com.kreditpintar.chatbot.ingestion.IngestionResult
import com.kreditpintar.chatbot.ingestion.TextChunker
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val logger = KotlinLogging.logger {}

data class IngestionResponse(
    @JsonProperty("document_id") val documentId: String,
    @JsonProperty("filename") val filename: String,
    @JsonProperty("doc_type") val docType: String,
    // "SUCCESS", "SKIPPED", or "FAILED"
    @JsonProperty("status") val status: String,
    @JsonProperty("chunks_created") val chunksCreated: Int,
    @JsonProperty("tokens_used") val tokensUsed: Int,
    @JsonProperty("duration_ms") val durationMs: Long,
    @JsonProperty("error_message") val errorMessage: String? = null,
)

data class BatchIngestionResponse(
    @JsonProperty("results") val results: List<IngestionResponse>,
    @JsonProperty("total_chunks_created") val totalChunksCreated: Int,
    @JsonProperty("total_tokens_used") val totalTokensUsed: Int,
    @JsonProperty("total_duration_ms") val totalDurationMs: Long,
)

data class DocumentSummary(
    @JsonProperty("document_id") val documentId: String,
    @JsonProperty("filename") val filename: String,
    @JsonProperty("doc_type") val docType: String,
    @JsonProperty("created_at") val createdAt: String,
    @JsonProperty("chunk_count") val chunkCount: Int,
)

data class DocumentListResponse(
    @JsonProperty("documents") val documents: List<DocumentSummary>,
    @JsonProperty("total") val total: Long,
)

@RestController
@RequestMapping("/admin")
class IngestionController(
    private val documentParser: DocumentParser,
    private val textChunker: TextChunker,
    private val embeddingService: EmbeddingService,
    private val properties: ChatbotProperties,
    private val documentRepository: DocumentRepository,
    private val chunkRepository: ChunkRepository,
) {
    @PostMapping("/ingest")
    fun ingestDocument(
        @RequestParam file: MultipartFile,
        @RequestParam(name = "doc_type") docType: String,
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

            return ResponseEntity.ok(mapIngestionResult(result, parsed.metadata["filename"] ?: "unknown", docType))
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid input")))
        } catch (e: Exception) {
            logger.error(e) { "Document ingestion failed" }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    IngestionResponse(
                        documentId = "error",
                        filename = file.originalFilename ?: "unknown",
                        docType = docType,
                        status = "FAILED",
                        chunksCreated = 0,
                        tokensUsed = 0,
                        durationMs = 0,
                        errorMessage = e.message,
                    ),
                )
        }
    }

    @PostMapping("/ingest/batch")
    fun ingestBatch(
        @RequestParam files: List<MultipartFile>,
        @RequestParam("doc_types") docTypes: List<String>,
        @RequestHeader("X-Admin-Key") adminKey: String,
    ): ResponseEntity<Any> {
        if (adminKey != properties.adminKey) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid admin key"))
        }

        val results = mutableListOf<IngestionResponse>()
        var totalChunks = 0
        var totalTokens = 0
        val startTime = System.currentTimeMillis()

        val validDocTypes = listOf("FAQ", "TOS", "BRAND", "HOWTO")
        if (docTypes.size != files.size) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "doc_types count (${docTypes.size}) must match files count (${files.size})"))
        }
        if (docTypes.any { it !in validDocTypes }) {
            return ResponseEntity.badRequest()
                .body(mapOf("error" to "All doc_types must be one of: $validDocTypes"))
        }

        for ((file, docType) in files.zip(docTypes)) {
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

                val resp = mapIngestionResult(result, parsed.metadata["filename"] ?: "unknown", docType)
                results.add(resp)
                totalChunks += result.chunksCreated
                totalTokens += result.tokensUsed
            } catch (e: Exception) {
                logger.error(e) { "Batch ingestion failed for file: ${file.originalFilename}" }
                results.add(
                    IngestionResponse(
                        documentId = "error",
                        filename = file.originalFilename ?: "unknown",
                        docType = docType,
                        status = "FAILED",
                        chunksCreated = 0,
                        tokensUsed = 0,
                        durationMs = 0,
                        errorMessage = e.message,
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

    @GetMapping("/documents")
    fun listDocuments(
        @RequestParam(defaultValue = "50") limit: Int,
        @RequestParam(required = false) doc_type: String?,
        @RequestHeader("X-Admin-Key") adminKey: String,
    ): ResponseEntity<Any> {
        if (adminKey != properties.adminKey) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "Invalid admin key"))
        }

        val pageable = PageRequest.of(0, limit.coerceIn(1, 200))
        val page =
            if (doc_type != null) {
                documentRepository.findByDocType(doc_type, pageable)
            } else {
                documentRepository.findAll(pageable)
            }

        val documents =
            page.content.map { doc ->
                DocumentSummary(
                    documentId = doc.id.toString(),
                    filename = doc.source,
                    docType = doc.docType,
                    createdAt = doc.createdAt.toString(),
                    chunkCount = chunkRepository.countByDocId(doc.id),
                )
            }

        return ResponseEntity.ok(
            DocumentListResponse(
                documents = documents,
                total = page.totalElements,
            ),
        )
    }

    private fun mapIngestionResult(
        result: IngestionResult,
        filename: String,
        docType: String,
    ): IngestionResponse {
        return IngestionResponse(
            documentId = result.documentId.toString(),
            filename = filename,
            docType = docType,
            status = if (result.skipped) "SKIPPED" else "SUCCESS",
            chunksCreated = result.chunksCreated,
            tokensUsed = result.tokensUsed,
            durationMs = result.durationMs,
        )
    }
}

package com.kreditpintar.chatbot.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DocumentRepository : JpaRepository<Document, UUID> {
    fun findByFileHash(fileHash: String): Optional<Document>

    fun existsByFileHash(fileHash: String): Boolean

    fun findByDocType(
        docType: String,
        pageable: Pageable,
    ): Page<Document>
}

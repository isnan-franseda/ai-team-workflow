# RAG Chunking & Retrieval Improvements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix "missing context" in pgvector RAG pipeline by improving chunking, context capacity, adjacent chunk retrieval, and conversation-aware search.

**Architecture:** Six targeted fixes applied in order: (1) whitespace normalization in TextChunker, (2) larger chunks + overlap, (3) docMaxChars aligned to actual capacity, (4) chunkIndex propagation for adjacent lookup, (5) adjacent chunk expansion in VectorSearchService, (6) conversation-aware search query in ChatService. No new dependencies.

**Tech Stack:** Kotlin, Spring Boot, JPA/Hibernate, pgvector (PostgreSQL), JUnit 5, Mockito

---

## File Map

| Action | File | Responsibility |
|--------|------|---------------|
| Modify | `backend/src/main/kotlin/.../ingestion/TextChunker.kt` | Add whitespace normalization before chunking |
| Modify | `backend/src/test/kotlin/.../ingestion/TextChunkerTest.kt` | Test whitespace normalization |
| Modify | `backend/src/main/resources/application.yml` | Update chunk-size, overlap, doc-max-chars, similarity-threshold |
| Modify | `backend/src/main/kotlin/.../domain/ChunkRepository.kt` | Add findAdjacentChunks native query |
| Modify | `backend/src/main/kotlin/.../pipeline/VectorSearchService.kt` | Add chunkIndex field, adjacent chunk expansion |
| Create | `backend/src/test/kotlin/.../pipeline/VectorSearchServiceTest.kt` | Test adjacent chunk expansion |
| Modify | `backend/src/main/kotlin/.../service/ChatService.kt` | Build conversation-aware search query |
| Modify | `backend/src/test/kotlin/.../service/ChatServiceTest.kt` | Test conversation-aware query |
| Create | `backend/src/main/resources/db/migration/V7__switch_to_hnsw_index.sql` | Replace IVFFlat with HNSW index |

---

## Task 1: TextChunker — Whitespace Normalization

**Files:**
- Modify: `backend/src/test/kotlin/com/kreditpintar/chatbot/ingestion/TextChunkerTest.kt`
- Modify: `backend/src/main/kotlin/com/kreditpintar/chatbot/ingestion/TextChunker.kt`

- [ ] **Step 1: Write failing tests**

Add to `TextChunkerTest.kt` inside the class body:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.ingestion.TextChunkerTest.multiple spaces*" --tests "com.kreditpintar.chatbot.ingestion.TextChunkerTest.excessive newlines*" -q 2>&1 | tail -20
```

Expected: FAIL (whitespace normalization not yet implemented)

- [ ] **Step 3: Add whitespace normalization to TextChunker**

In `TextChunker.kt`, replace the `chunk` function body so the first statement normalizes text:

```kotlin
fun chunk(text: String): List<TextChunk> {
    val normalizedText = text
        .replace(Regex("[ \t]{2,}"), " ")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()

    val chunkSize = properties.chunking.chunkSize
    val overlap = properties.chunking.overlap

    if (normalizedText.isEmpty()) return emptyList()

    if (normalizedText.length <= chunkSize) {
        return listOf(
            TextChunk(
                text = normalizedText,
                index = 0,
                tokenCount = estimateTokens(normalizedText),
            ),
        )
    }

    val chunks = mutableListOf<TextChunk>()
    var index = 0
    var position = 0

    while (position < normalizedText.length) {
        val end = findChunkEnd(normalizedText, position, chunkSize)
        val chunkText = normalizedText.substring(position, end).trim()

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
            if (end >= normalizedText.length) {
                normalizedText.length
            } else {
                end - overlap
            }
    }

    return chunks
}
```

- [ ] **Step 4: Run all TextChunker tests**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.ingestion.TextChunkerTest" -q 2>&1 | tail -20
```

Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/com/kreditpintar/chatbot/ingestion/TextChunker.kt \
        backend/src/test/kotlin/com/kreditpintar/chatbot/ingestion/TextChunkerTest.kt
git commit -m "feat: normalize whitespace in TextChunker before chunking"
```

---

## Task 2: Config — Larger Chunks + Higher Thresholds + More Context Capacity

**Files:**
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Update application.yml**

In `application.yml`, apply all config changes:

```yaml
# Change chunking section (was chunk-size: 400, overlap: 50)
  chunking:
    chunk-size: 800
    overlap: 100

# Change search section (was similarity-threshold: 0.3)
  search:
    top-k: 5
    similarity-threshold: ${SIMILARITY_THRESHOLD:0.5}
    max-web-results: 3

# Change context section (was doc-max-chars: 1200)
  context:
    max-history-messages: ${MAX_HISTORY_MESSAGES:5}
    doc-weight-percent: 60
    web-weight-percent: 30
    max-context-chars: ${MAX_CONTEXT_CHARS:4000}
    doc-max-chars: ${DOC_MAX_CHARS:3000}
    web-max-chars: ${WEB_MAX_CHARS:600}
```

- [ ] **Step 2: Verify app compiles**

```bash
cd backend && ./gradlew compileKotlin -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/application.yml
git commit -m "config: increase chunk size to 800, doc-max-chars to 3000, threshold to 0.5"
```

---

## Task 3: VectorSearchResult — Add chunkIndex Field

**Files:**
- Modify: `backend/src/main/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchService.kt`

Context: The SQL query already returns `c.chunk_index` at position row[3], but it was silently skipped in the mapping. We need it for adjacent chunk lookup.

- [ ] **Step 1: Add chunkIndex to VectorSearchResult (default = 0 to avoid breaking callers)**

In `VectorSearchService.kt`, update the data class and the row mapping:

```kotlin
data class VectorSearchResult(
    val chunkId: UUID,
    val docId: UUID,
    val chunkText: String,
    val chunkIndex: Int = 0,
    val source: String,
    val docType: String,
    val similarity: Double,
)
```

Update the row mapping in `search()`:

```kotlin
return results
    .map { row ->
        VectorSearchResult(
            chunkId = row[0] as UUID,
            docId = row[1] as UUID,
            chunkText = row[2] as String,
            chunkIndex = (row[3] as Number).toInt(),
            source = row[4] as String,
            docType = row[5] as String,
            similarity = (row[6] as Number).toDouble(),
        )
    }
    .filter { it.similarity >= properties.search.similarityThreshold }
    .also { filtered ->
        logger.info { "Vector search returned ${filtered.size} results above threshold ${properties.search.similarityThreshold}" }
    }
```

- [ ] **Step 2: Verify compile**

```bash
cd backend && ./gradlew compileKotlin compileTestKotlin -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL (chunkIndex has default = 0, so existing test code that constructs VectorSearchResult without it still compiles)

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchService.kt
git commit -m "feat: add chunkIndex to VectorSearchResult for adjacent chunk lookup"
```

---

## Task 4: ChunkRepository — Add Adjacent Chunk Query

**Files:**
- Modify: `backend/src/main/kotlin/com/kreditpintar/chatbot/domain/ChunkRepository.kt`

- [ ] **Step 1: Add findAdjacentChunks native query**

In `ChunkRepository.kt`, add after `findTopKBySimilarity`:

```kotlin
@Query(
    nativeQuery = true,
    value = """
        SELECT c.id, c.doc_id, c.chunk_text, c.chunk_index, d.source, d.doc_type
        FROM chunks c
        JOIN documents d ON c.doc_id = d.id
        WHERE c.doc_id = :docId
          AND c.chunk_index BETWEEN :startIndex AND :endIndex
        ORDER BY c.chunk_index
    """,
)
fun findAdjacentChunks(
    @Param("docId") docId: UUID,
    @Param("startIndex") startIndex: Int,
    @Param("endIndex") endIndex: Int,
): List<Array<Any>>
```

- [ ] **Step 2: Verify compile**

```bash
cd backend && ./gradlew compileKotlin -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/kreditpintar/chatbot/domain/ChunkRepository.kt
git commit -m "feat: add findAdjacentChunks query to ChunkRepository"
```

---

## Task 5: VectorSearchService — Adjacent Chunk Expansion

**Files:**
- Create: `backend/src/test/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchServiceTest.kt`
- Modify: `backend/src/main/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchService.kt`

The adjacent expansion: for each high-confidence result (similarity ≥ 0.6), fetch chunks at index-1 and index+1 from the same document, then append any not already in results.

- [ ] **Step 1: Write failing test**

Create `backend/src/test/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchServiceTest.kt`:

```kotlin
package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.EmbeddingClient
import com.kreditpintar.chatbot.config.EmbeddingResult
import com.kreditpintar.chatbot.domain.ChunkRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.util.UUID

class VectorSearchServiceTest {
    private lateinit var embeddingClient: EmbeddingClient
    private lateinit var chunkRepository: ChunkRepository
    private lateinit var properties: ChatbotProperties
    private lateinit var vectorSearchService: VectorSearchService

    private val docId = UUID.randomUUID()
    private val chunkId = UUID.randomUUID()
    private val adjChunkId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        embeddingClient = mock()
        chunkRepository = mock()
        properties = ChatbotProperties().apply {
            search.topK = 5
            search.similarityThreshold = 0.5
        }
        vectorSearchService = VectorSearchService(embeddingClient, chunkRepository, properties)

        whenever(embeddingClient.embed(any())).thenReturn(
            listOf(EmbeddingResult(embedding = List(1024) { 0.1f }, tokensUsed = 10))
        )
    }

    @Test
    fun `search returns results above threshold with chunkIndex`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Chunk text", 2, "doc.pdf", "FAQ", 0.85)
            )
        )
        whenever(chunkRepository.findAdjacentChunks(any(), any(), any())).thenReturn(emptyList())

        val results = vectorSearchService.search("test query")

        assertEquals(1, results.size)
        assertEquals(2, results[0].chunkIndex)
        assertEquals(0.85, results[0].similarity, 0.001)
    }

    @Test
    fun `search expands adjacent chunks for high-confidence results`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Main chunk text", 2, "doc.pdf", "FAQ", 0.85)
            )
        )
        whenever(chunkRepository.findAdjacentChunks(eq(docId), eq(1), eq(3))).thenReturn(
            listOf(
                arrayOf(adjChunkId, docId, "Adjacent chunk text", 1, "doc.pdf", "FAQ")
            )
        )

        val results = vectorSearchService.search("test query")

        assertEquals(2, results.size)
        assertTrue(results.any { it.chunkId == chunkId })
        assertTrue(results.any { it.chunkId == adjChunkId })
    }

    @Test
    fun `search does not duplicate chunks already in results`() {
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(chunkId, docId, "Chunk 2", 2, "doc.pdf", "FAQ", 0.85),
                arrayOf(adjChunkId, docId, "Chunk 1", 1, "doc.pdf", "FAQ", 0.72),
            )
        )
        whenever(chunkRepository.findAdjacentChunks(any(), any(), any())).thenReturn(
            listOf(
                arrayOf(adjChunkId, docId, "Chunk 1", 1, "doc.pdf", "FAQ")
            )
        )

        val results = vectorSearchService.search("test query")

        assertEquals(2, results.size)
        assertEquals(1, results.count { it.chunkId == adjChunkId })
    }

    @Test
    fun `search does not expand adjacent for low-confidence results`() {
        val lowConfidenceId = UUID.randomUUID()
        whenever(chunkRepository.findTopKBySimilarity(any(), eq(5))).thenReturn(
            listOf(
                arrayOf(lowConfidenceId, docId, "Low conf chunk", 2, "doc.pdf", "FAQ", 0.55)
            )
        )

        val results = vectorSearchService.search("test query")

        assertEquals(1, results.size)
        org.mockito.kotlin.verify(chunkRepository, org.mockito.kotlin.never())
            .findAdjacentChunks(any(), any(), any())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.pipeline.VectorSearchServiceTest" -q 2>&1 | tail -20
```

Expected: FAIL (expandWithAdjacentChunks not yet implemented)

- [ ] **Step 3: Add adjacent expansion to VectorSearchService**

In `VectorSearchService.kt`, update the `search` function and add private helper:

```kotlin
fun search(query: String): List<VectorSearchResult> {
    val queryEmbedding =
        embeddingClient.embed(listOf(query)).firstOrNull()
            ?: run {
                logger.warn { "Failed to embed query: $query" }
                return emptyList()
            }

    val queryVectorStr = queryEmbedding.embedding.joinToString(",", "[", "]")
    val results =
        chunkRepository.findTopKBySimilarity(
            queryVector = queryVectorStr,
            limit = properties.search.topK,
        )

    val filtered = results
        .map { row ->
            VectorSearchResult(
                chunkId = row[0] as UUID,
                docId = row[1] as UUID,
                chunkText = row[2] as String,
                chunkIndex = (row[3] as Number).toInt(),
                source = row[4] as String,
                docType = row[5] as String,
                similarity = (row[6] as Number).toDouble(),
            )
        }
        .filter { it.similarity >= properties.search.similarityThreshold }

    val expanded = expandWithAdjacentChunks(filtered)

    logger.info { "Vector search: ${filtered.size} direct results, ${expanded.size} after adjacent expansion" }
    return expanded
}

private fun expandWithAdjacentChunks(results: List<VectorSearchResult>): List<VectorSearchResult> {
    val existingIds = results.map { it.chunkId }.toMutableSet()
    val adjacent = mutableListOf<VectorSearchResult>()

    results
        .filter { it.similarity >= ADJACENT_EXPANSION_THRESHOLD }
        .forEach { result ->
            chunkRepository
                .findAdjacentChunks(
                    docId = result.docId,
                    startIndex = result.chunkIndex - 1,
                    endIndex = result.chunkIndex + 1,
                )
                .forEach { row ->
                    val id = row[0] as UUID
                    if (id !in existingIds) {
                        existingIds.add(id)
                        adjacent.add(
                            VectorSearchResult(
                                chunkId = id,
                                docId = row[1] as UUID,
                                chunkText = row[2] as String,
                                chunkIndex = (row[3] as Number).toInt(),
                                source = row[4] as String,
                                docType = row[5] as String,
                                similarity = 0.0,
                            ),
                        )
                    }
                }
        }

    return results + adjacent
}

companion object {
    private const val ADJACENT_EXPANSION_THRESHOLD = 0.6
}
```

- [ ] **Step 4: Run all VectorSearchService tests**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.pipeline.VectorSearchServiceTest" -q 2>&1 | tail -20
```

Expected: All 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchService.kt \
        backend/src/test/kotlin/com/kreditpintar/chatbot/pipeline/VectorSearchServiceTest.kt
git commit -m "feat: expand vector search results with adjacent chunks for high-confidence matches"
```

---

## Task 6: ChatService — Conversation-Aware Search Query

**Files:**
- Modify: `backend/src/test/kotlin/com/kreditpintar/chatbot/service/ChatServiceTest.kt`
- Modify: `backend/src/main/kotlin/com/kreditpintar/chatbot/service/ChatService.kt`

Context: Currently only the raw user message is embedded for search. Follow-up questions like "what about the fee?" lack subject context. Fix: prepend last 2 message contents from history (max 200 chars) to the search query.

Note: This requires fetching history BEFORE vector search, breaking the current parallel execution. The history fetch is a fast DB query (~5ms); this trade-off is acceptable since it enables disambiguation.

- [ ] **Step 1: Add test for conversation-aware query**

Add to `ChatServiceTest.kt`:

```kotlin
@Test
fun `chat uses conversation-aware query when history exists`() {
    val sessionId = UUID.randomUUID()
    val userMessage = "berapa biayanya?"

    val history = listOf(
        Message(sessionId = sessionId, role = "user", content = "saya ingin tahu tentang limit pinjaman"),
        Message(sessionId = sessionId, role = "assistant", content = "Limit pinjaman maksimal Rp 20.000.000"),
    )

    whenever(sessionService.getConversationHistory(sessionId)).thenReturn(history)
    whenever(vectorSearchService.search(any())).thenReturn(emptyList())
    whenever(contextAssembler.assemble(any())).thenReturn(
        AssembledContext(context = "", citations = emptyList())
    )
    whenever(chatClient.chat(any(), any(), any(), any(), anyOrNull())).thenReturn(
        ChatResult(content = "Biaya administrasi adalah 1%.")
    )
    whenever(outputValidatorService.validate(any(), any())).thenReturn(
        ValidationResult(passed = true, reason = null)
    )
    whenever(sessionService.saveMessage(any(), any(), any())).thenReturn(
        Message(sessionId = sessionId, role = "user", content = userMessage)
    )

    val captor = org.mockito.kotlin.argumentCaptor<String>()
    chatService.chat(sessionId, userMessage)

    org.mockito.kotlin.verify(vectorSearchService).search(captor.capture())
    assertTrue(captor.firstValue.contains("berapa biayanya?"))
    assertTrue(captor.firstValue.length > userMessage.length)
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.service.ChatServiceTest.chat uses conversation*" -q 2>&1 | tail -20
```

Expected: FAIL (search still called with raw userMessage)

- [ ] **Step 3: Update ChatService to use conversation-aware query**

In `ChatService.kt`, replace the parallel execution block with sequential history-first approach:

```kotlin
fun chat(
    sessionId: UUID,
    userMessage: String,
): ChatMessageResponse {
    val startTime = System.currentTimeMillis()

    // Step 1: Fetch history first (needed to build augmented search query)
    val history = sessionService.getConversationHistory(sessionId)
    val historyPairs = history.map { Pair(it.role.lowercase(), it.content) }

    // Step 2: Build conversation-aware search query and run vector search
    val searchQuery = buildSearchQuery(userMessage, historyPairs)
    val docResults: List<VectorSearchResult> =
        try {
            vectorSearchService.search(searchQuery)
        } catch (e: Exception) {
            logger.error(e) { "Vector search failed for session=$sessionId" }
            emptyList()
        }

    // Step 3: Assemble context from document results
    val assembledContext = contextAssembler.assemble(docResults)

    // Step 4: Generate response via ChatClient
    val chatResult =
        try {
            chatClient.chat(
                systemPrompt = KpSystemPrompt.SYSTEM_PROMPT,
                context = assembledContext.context,
                history = historyPairs,
                userMessage = userMessage,
            )
        } catch (e: Exception) {
            logger.error(e) { "Chat generation failed for session=$sessionId" }
            return ChatMessageResponse(
                sessionId = sessionId,
                response = properties.fallbackMessage,
                citations = emptyList(),
                responseTimeMs = System.currentTimeMillis() - startTime,
                timestamp = Instant.now().toString(),
            )
        }

    // Step 5: Output validation
    val validationResult = outputValidatorService.validate(chatResult.content, sessionId.toString())

    val finalResponse =
        if (validationResult.passed) {
            chatResult.content
        } else {
            logger.warn { "Output validation failed for session=$sessionId: ${validationResult.reason}" }
            properties.fallbackMessage
        }

    // Step 6: Persist messages
    sessionService.saveMessage(sessionId, "user", userMessage)
    sessionService.saveMessage(sessionId, "assistant", finalResponse)

    // Step 7: Build response with citations
    val sources =
        assembledContext.citations.map {
            SourceInfo(source = it.source, type = it.type)
        }

    return ChatMessageResponse(
        sessionId = sessionId,
        response = finalResponse,
        citations = sources,
        responseTimeMs = System.currentTimeMillis() - startTime,
        timestamp = Instant.now().toString(),
    )
}

private fun buildSearchQuery(userMessage: String, history: List<Pair<String, String>>): String {
    if (history.isEmpty()) return userMessage
    val recentContext = history.takeLast(2).joinToString(" ") { it.second }.take(200)
    return "$recentContext $userMessage".take(500)
}
```

Also remove the now-unused `CompletableFuture` import.

- [ ] **Step 4: Run all ChatService tests**

```bash
cd backend && ./gradlew test --tests "com.kreditpintar.chatbot.service.ChatServiceTest" -q 2>&1 | tail -20
```

Expected: All tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/kotlin/com/kreditpintar/chatbot/service/ChatService.kt \
        backend/src/test/kotlin/com/kreditpintar/chatbot/service/ChatServiceTest.kt
git commit -m "feat: build conversation-aware search query from recent message history"
```

---

## Task 7: HNSW Index Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__switch_to_hnsw_index.sql`

Context: IVFFlat requires ~39×lists rows for quality search. With few documents this degrades retrieval quality. HNSW works well at any dataset size. Since re-ingestion of all documents is required anyway (chunk-size change), this migration runs at the same time with no extra cost.

- [ ] **Step 1: Create migration**

Create `backend/src/main/resources/db/migration/V7__switch_to_hnsw_index.sql`:

```sql
-- Replace IVFFlat with HNSW for better recall at any dataset size
DROP INDEX IF EXISTS idx_chunks_embedding_cosine;

CREATE INDEX idx_chunks_embedding_hnsw ON chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

- [ ] **Step 2: Verify compile (Flyway migration is picked up at runtime)**

```bash
cd backend && ./gradlew compileKotlin -q 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V7__switch_to_hnsw_index.sql
git commit -m "feat: replace IVFFlat with HNSW index for better recall on small datasets"
```

---

## Task 8: Run Full Test Suite

- [ ] **Step 1: Run all backend tests**

```bash
cd backend && ./gradlew test -q 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 2: Re-ingest all documents**

After deploying, all existing documents must be re-ingested (chunk-size doubled from 400→800, so existing embeddings were generated with old chunk boundaries). Delete all documents and chunks, then re-upload:

```bash
# Via admin API
curl -X DELETE http://localhost:8080/admin/documents/all \
  -H "X-Admin-Key: ${ADMIN_API_KEY}"
# Then re-upload all source documents via the ingestion endpoint
```

---

## Verification

1. `./gradlew test` — all tests pass
2. Re-ingest documents (see Task 8 Step 2)
3. Manual test: ask a multi-sentence question about loan eligibility → LLM should return answer with surrounding context from adjacent chunks
4. Manual test: ask follow-up "berapa biayanya?" after a conversation about loans → search should find fee-related chunks
5. Check logs: `Vector search: X direct results, Y after adjacent expansion` — Y should be > X for relevant queries
6. Check `ingestion_logs` table: `chunks_created` should be roughly half the old count (each chunk is ~2× larger)

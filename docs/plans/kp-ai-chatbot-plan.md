# KP AI Chatbot Backend MVP — Implementation Plan

> **Status:** Draft
> **Date:** 2026-05-06
> **Related:** [PRD](../prd/kp-ai-chatbot.md) | [Architecture](../architecture.md) | [Spec](../../specs/kp-ai-chatbot/spec.md)

---

## 1. Context

Greenfield Spring Boot 3 / Kotlin backend for the KP AI Chatbot — a RAG-based conversational assistant for Kredit Pintar borrowers. Combines document knowledge (pgvector) with live web search, filtered through a values-alignment layer.

**Zero source code exists.** All deliverables built from scratch per existing architecture docs.

### Constraints (from AGENTS.md NEVER rules)
- NEVER run Flyway migrations outside dev Docker (`make dev-up`)
- NEVER call MiniMax API directly — always via `MiniMaxClient.kt`
- NEVER inject web search results into LLM context without `ValuesFilterService`
- NEVER return a chat response without `OutputValidatorService`
- NEVER change `KpSystemPrompt.kt` or `ValuesFilterPrompt.kt` without human review
- Kotlin only, no Java. Strict null safety — no `!!` without explanatory comment
- No `println()` or `System.out` — SLF4J Logger only

### Data Model
```
documents(id UUID, source VARCHAR, doc_type VARCHAR, file_hash VARCHAR, created_at TIMESTAMP)
chunks(id UUID, doc_id FK, chunk_text TEXT, chunk_index INT, token_count INT, embedding vector(1536), created_at TIMESTAMP)
sessions(id UUID, created_at TIMESTAMP, last_active_at TIMESTAMP)
messages(id UUID, session_id FK, role VARCHAR, content TEXT, created_at TIMESTAMP)
```

### API Endpoints
| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/chat/session` | None | Create session |
| POST | `/chat/message` | None | Send message, get AI response |
| GET | `/chat/history/{sessionId}` | None | Conversation history |
| POST | `/admin/ingest` | X-Admin-Key | Single document ingestion |
| POST | `/admin/ingest/batch` | X-Admin-Key | Multi-document ingestion |
| GET | `/actuator/health` | None | Health check |

---

## 2. Implementation Steps

### Step 1: Project Scaffold + Build Config + Docker Compose

**Goal:** Bootable Spring Boot 3 / Kotlin project with all dependencies, working local PostgreSQL + pgvector via Docker.

**Files:**
- `build.gradle.kts` — root build file
- `settings.gradle.kts` — project settings
- `gradle.properties` — Gradle JVM args
- `docker-compose.yml` — pgvector container
- `Makefile` — dev targets (`dev-up`, `dev-down`, `dev-reset`)
- `src/main/kotlin/com/kreditpintar/chatbot/KpChatbotApplication.kt`
- `src/main/resources/application.yml` — base Spring config
- `src/main/resources/application-dev.yml` — dev profile
- `src/test/resources/application-test.yml` — test profile

**Key dependencies:** Spring Boot 3 (web, data-jpa, actuator), Kotlin, Flyway, PostgreSQL, pgvector, OkHttp3, Bucket4j, Apache PDFBox, Apache POI, kotlin-logging, Testcontainers, springmockk

**Docker Compose:** pgvector image, port 5432, volume `pgdata`, db `kp_chatbot`, user `kp_user`

**Verification:**
1. `./gradlew build` compiles clean
2. `make dev-up` → pgvector healthy
3. `./gradlew bootRun --args='--spring.profiles.active=dev'` → app on 8080
4. `GET /actuator/health` → `{"status": "UP"}`
5. `make dev-down` stops cleanly

---

### Step 2: Flyway Migrations

**Files:**
- `V1__create_documents_table.sql` — id UUID PK, source, doc_type, file_hash UNIQUE, created_at
- `V2__create_chunks_table.sql` — CREATE EXTENSION vector, id UUID PK, doc_id FK, chunk_text, chunk_index, token_count, embedding vector(1536), created_at, ivfflat index
- `V3__create_sessions_table.sql` — id UUID PK, created_at, last_active_at
- `V4__create_messages_table.sql` — id UUID PK, session_id FK, role CHECK, content, created_at, index on (session_id, created_at)

**Verification:**
1. `make dev-reset` + start app → Flyway applies 4 migrations
2. `\dt` in psql shows all tables
3. `\d chunks` shows vector(1536) + ivfflat index
4. `flyway_schema_history` shows 4 success entries

---

### Step 3: Domain Entities + Spring Data Repositories

**Files:**
- `domain/Document.kt` — @Entity, id, source, docType, fileHash, createdAt
- `domain/Chunk.kt` — @Entity, id, docId FK, chunkText, chunkIndex, tokenCount, embedding (UserType for pgvector), createdAt
- `domain/Session.kt` — @Entity, id, createdAt, lastActiveAt
- `domain/Message.kt` — @Entity, id, sessionId FK, role enum, content, createdAt
- `domain/DocumentRepository.kt` — findByFileHash, findByDocType
- `domain/ChunkRepository.kt` — native cosine similarity query (`<=>`), findByDocIdOrderByChunkIndex
- `domain/SessionRepository.kt` — standard CRUD
- `domain/MessageRepository.kt` — findBySessionIdOrderByCreatedAtDesc (Pageable)

**Verification:**
1. `@DataJpaTest` for DocumentRepository: save/retrieve by fileHash, uniqueness
2. `@DataJpaTest` for ChunkRepository: cosine search returns ordered results
3. `@DataJpaTest` for MessageRepository: query last 10 of 15
4. `./gradlew ktlintCheck` clean

---

### Step 4: MiniMaxClient (OkHttp3 Wrapper)

**Goal:** Single centralized HTTP client for ALL MiniMax API calls. Retry logic, rate-limit awareness, structured errors. All other services MUST use this.

**Files:**
- `config/MiniMaxClient.kt` — @Component, OkHttp3 wrapper
- `config/MiniMaxProperties.kt` — @ConfigurationProperties("minimax")
- `config/MiniMaxApiException.kt` — sealed class hierarchy

**Public methods:**
1. `embed(texts: List<String>): List<FloatArray>` — embo-01, returns 1536-dim vectors
2. `chat(messages: List<ChatMessage>, systemPrompt: String, tools: List<ToolDef>?): ChatResponse` — MiniMax-Text-01
3. `chatWithWebSearch(query: String): WebSearchResult` — web search tool invocation

**Config:** OkHttpClient with connection pool, timeouts, Auth interceptor (Bearer token), retry interceptor (exponential backoff 1s/2s/4s, max 3 retries for 5xx/network errors)

**Error handling:** RateLimitException, AuthException, ServerException, TimeoutException, NetworkException

**Verification:**
1. MockWebServer: embed returns known vectors, verified
2. MockWebServer: 429 → retry succeeds on 2nd attempt
3. MockWebServer: 500 x3 → ServerException after max retries
4. Timeout → TimeoutException
5. Auth header present in all requests

---

### Step 5: Ingestion Pipeline

**Goal:** Full document ingestion: parse → chunk → embed → store in pgvector. Idempotent by SHA-256 file hash.

**Files:**
- `ingestion/DocumentParser.kt` — PDF (PDFBox), DOCX (POI), TXT parsing
- `ingestion/TextChunker.kt` — ~400-char chunks, 50-char overlap, Indonesian sentence boundaries
- `ingestion/EmbeddingService.kt` — batch embedding (10 chunks/batch)
- `ingestion/IngestionService.kt` — orchestrator: hash check → parse → chunk → embed → persist
- `api/IngestionController.kt` — POST /admin/ingest, POST /admin/ingest/batch
- `api/dto/IngestionRequest.kt`, `api/dto/IngestionResult.kt`

**Flow:**
1. Compute SHA-256 of file bytes
2. Check `documentRepository.findByFileHash(hash)` → skip if exists
3. Parse document → plain text
4. Chunk text → List<ChunkResult>
5. Embed chunks in batches of 10 → List<ChunkEmbedding>
6. Persist Document + Chunks to DB
7. Return `IngestionResult(chunksCreated, tokensUsed, skipped, durationMs)`

**Verification:**
1. Parse sample PDF/DOCX/TXT → text extracted
2. Parse unsupported type → exception
3. Parse empty file → empty text, no crash
4. Chunk 1000-char text → ~400-char chunks with overlap
5. First upload → chunks created, non-skipped
6. Duplicate upload → skipped, no new chunks
7. Integration test: missing X-Admin-Key → 401, invalid docType → 400

---

### Step 6: Chat Pipeline Services

**Goal:** Five stateless pipeline components that ChatService orchestrates.

**Files:**
- `pipeline/VectorSearchService.kt` — cosine similarity on query embedding, top-K=5, min threshold 0.7
- `pipeline/WebSearchService.kt` — MiniMax web search tool, max 3 results
- `pipeline/ValuesFilterService.kt` — evaluates each web result via MiniMax-Text-01 against KP values; only PASS results forwarded
- `pipeline/ContextAssembler.kt` — merges 60% doc chunks (~1800 chars) + 30% web (~900 chars) into structured context
- `pipeline/OutputValidatorService.kt` — validates final response: no loan promises, no fear tactics, no competitor promotion, BI language
- `pipeline/dto/` — SearchResult, FilterResult, ValidationResult, AssembledContext

**Critical mandates:**
- ValuesFilterService: NEVER bypass. FAIL results logged with session ID.
- OutputValidatorService: NEVER return unvalidated response. FAIL → safe fallback.

**Verification:** Unit tests for each service with mocked dependencies. Test edge cases: empty results, threshold filtering, mixed PASS/FAIL, budget constraints.

---

### Step 7: ChatService Orchestrator

**Goal:** Central orchestrator for the full chat pipeline.

**Files:**
- `service/ChatService.kt` — processMessage(sessionId, userMessage)
- `service/SessionService.kt` — CRUD for sessions, message history, lastActiveAt updates

**Full orchestration flow:**
1. Validate session exists
2. Save user message
3. Embed query (parallel) — `miniMaxClient.embed()`
4. Vector search (parallel) — `vectorSearchService.search()`
5. Web search (parallel) — `webSearchService.search()`
6. Values filter web results → `valuesFilterService.filter()`
7. Get recent 10 messages from history
8. Assemble context → `contextAssembler.assemble()`
9. Chat completion → `miniMaxClient.chat()` with KP system prompt + context + history
10. Output validation → `outputValidatorService.validate()`
11. If FAIL → safe fallback message (BI), log failure
12. Save assistant message
13. Return `ChatMessageResponse(response, sources[], sessionId, messageId, durationMs)`

**Safe fallback:** "Maaf, saya tidak dapat memberikan jawaban yang tepat untuk pertanyaan Anda saat ini. Silakan hubungi tim dukungan Kredit Pintar untuk bantuan lebih lanjut."

**Parallel execution:** Steps 3+4+5 launched concurrently via Kotlin coroutines (`async/await`). Overall 30s timeout.

**DTOs:** ChatMessageRequest, ChatMessageResponse, Source (type, title, url, snippet), SessionResponse

**Verification:**
1. Unit test: mocked pipeline, verify orchestrator call order
2. Validator FAIL → safe fallback returned
3. Values filter rejects all → no web sources in context
4. No doc chunks → web-only context
5. MiniMaxClient throws → graceful error, no message saved
6. Integration test (Testcontainers): end-to-end session → message → response with sources
7. Session continuity: 3 messages, 4th with anaphora → context retained

---

### Step 8: REST Controllers + Rate Limiting

**Files:**
- `api/ChatController.kt` — POST /chat/message
- `api/SessionController.kt` — POST /chat/session, GET /chat/history/{sessionId}
- `api/HealthController.kt` — relies on Actuator
- `api/GlobalExceptionHandler.kt` — @RestControllerAdvice
- `api/dto/` — ChatMessageRequest, ChatMessageResponse, SessionResponse, ErrorResponse
- `config/RateLimitConfig.kt` — Bucket4j: 20 tokens/min/session, ConcurrentHashMap<UUID, Bucket>
- `config/RateLimitFilter.kt` — intercepts /chat/message, returns 429 when exhausted

**Error mapping:**
- SessionNotFoundException → 404
- InvalidDocumentTypeException → 400
- UnauthorizedException → 401
- MiniMax RateLimitException → 503 "Layanan sedang sibuk"
- MiniMax ServerException → 502
- MiniMax TimeoutException → 504
- Generic Exception → 500 (no PII in body)

**Rate limiting:** In-memory bucket store, stale cleanup every 15 min. `X-RateLimit-Remaining` header on responses.

**Verification:**
1. POST /chat/session → 201 with UUID
2. POST /chat/message → 200 with response + sources
3. Missing sessionId → 400
4. Non-existent session → 404
5. 20 requests → all succeed, 21st → 429
6. Different sessions have independent rate limits
7. Error responses have structured JSON, no stack traces

---

### Step 9: Configuration + System Prompts

**Files:**
- `config/KpSystemPrompt.kt` — full BI system prompt: brand voice, responsible lending, citation rules, language, out-of-scope handling
- `config/ValuesFilterPrompt.kt` — evaluation prompt for values filter: JSON output `{result: PASS|FAIL, reason: "..."}`
- `config/AppProperties.kt` — @ConfigurationProperties("app"): admin.key, vector.minSimilarity, context max sizes, rate limit config
- `config/AdminKeyValidator.kt` — constant-time comparison with configured admin key
- `application.yml` (updated), `application-dev.yml` (updated)
- `.env.example` — template for MINIMAX_API_KEY, ADMIN_API_KEY

**NEVER change KpSystemPrompt.kt or ValuesFilterPrompt.kt without human review.**

**Verification:**
1. AdminKeyValidator: correct key → true, wrong → false, constant-time check
2. AppProperties bind correctly with defaults
3. Env var overrides take effect
4. `.env` and `application-prod.yml` in `.gitignore`

---

### Step 10: Tests

**Goal:** Comprehensive coverage. All Gherkin acceptance criteria from PRD Section 7 covered.

**Test infrastructure:**
- Testcontainers for PostgreSQL + pgvector in integration tests
- MockWebServer (OkHttp) for MiniMax API mocking
- Test fixtures: sample-faq.pdf, sample-faq.docx, sample-faq.txt, corrupted.pdf

**Key integration tests:**
1. Complete chat flow (Testcontainers + mocked MiniMax): session → message → response with citation
2. Session continuity: 4-turn conversation with anaphora
3. Rate limit enforcement: 20 ok, 21st → 429
4. Output validator fallback: mocked FAIL → safe fallback returned
5. Web search values filter: competitor → NOT in context
6. Ingestion + query: upload doc → query → doc chunk cited in response

**Coverage targets:** >= 80% line, >= 70% branch on business logic packages

**Verification:** `./gradlew test` passes, `./gradlew ktlintCheck` clean

---

## 3. Sequencing & Dependencies

```
Step 1 (Scaffold + Docker)
  └── Step 2 (Flyway Migrations)
        └── Step 3 (Domain Entities + Repos)
              └── Step 4 (MiniMaxClient)
                    ├── Step 5 (Ingestion Pipeline)
                    └── Step 6 (Chat Pipeline Services)
                          ├── Step 7 (ChatService Orchestrator)
                          │     └── Step 8 (Controllers + Rate Limiting)
                          └── Step 9 (Configuration + Prompts)
Step 10 (Tests) — spans all, final integration tests blocked by 5-8
```

**Parallelization:** Steps 2+3 together, Steps 5+6 in parallel after Step 4, Step 9 alongside 5-8.

---

## 4. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| MiniMax API changes | High | Single abstraction in MiniMaxClient.kt |
| ivfflat index not built | Medium | V2 migration includes it; verify in CI |
| Values filter latency (LLM call per web result) | Medium | Run evaluations in parallel (3 concurrent max) |
| Bucket4j in-memory lost on restart | Low (MVP) | Acceptable; Phase 2 → Redis |
| Complex PDF parsing failures | Medium | Graceful degradation: empty text + warning |
| Duplicate upload embedding cost | Medium | SHA-256 hash idempotency |

---

## 5. Environment Variables

| Variable | Required | Purpose |
|----------|----------|---------|
| `MINIMAX_API_KEY` | Yes | MiniMax API auth |
| `ADMIN_API_KEY` | Yes | Admin endpoint auth |
| `SPRING_PROFILES_ACTIVE` | No | Profile selection |
| `DB_URL` | No | JDBC URL |
| `DB_USER` | No | DB username |
| `DB_PASSWORD` | No | DB password |
| `MINIMAX_BASE_URL` | No | API URL override |
| `SERVER_PORT` | No | HTTP port |

---

## 6. PRD Acceptance Criteria Traceability

| PRD Scenario | Covered By |
|--------------|-----------|
| FAQ query answered from document knowledge | Step 7 ChatService, Step 10 ChatFlowIntegrationTest |
| Web search result fails values filter | Step 6 ValuesFilterService, Step 10 integration test |
| Output validator flags misleading response | Step 6 OutputValidatorService, Step 7 fallback, Step 10 test |
| Session continuity across multiple turns | Step 7 history injection, Step 10 session continuity test |

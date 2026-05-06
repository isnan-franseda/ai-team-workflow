# KP AI Chatbot — Architecture

## Overview

The KP AI Chatbot is a RAG (Retrieval-Augmented Generation) system that answers
end-user questions by combining knowledge from KP's internal documents
(FAQs, T&C, brand guidelines, how-to guides) with live web search results,
filtered through a values alignment layer that enforces KP's brand and
responsible lending principles. Built on Spring Boot 3 / Kotlin, backed by
PostgreSQL + pgvector, and powered entirely by MiniMax APIs.

---

## Component Diagram

```
┌────────────────────────────────────────────────────────────────┐
│                         END USER                               │
└──────────────────────────┬─────────────────────────────────────┘
                           │ POST /chat/message
┌──────────────────────────▼─────────────────────────────────────┐
│                    ChatController (REST API)                    │
│             Rate limit: Bucket4j 20 req/min/session            │
└──────────────────────────┬─────────────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────────────┐
│                      ChatService (Orchestrator)                 │
│  1. embed query   2. similarity search   3. web search         │
│  4. values filter   5. assemble context   6. generate          │
│  7. validate output   8. persist   9. return                   │
└───┬───────────────────┬───────────────────────────┬────────────┘
    │                   │                           │
    ▼                   ▼                           ▼
┌───────────┐  ┌─────────────────┐    ┌────────────────────────┐
│  Vector   │  │   WebSearch     │    │   MiniMax-Text-01      │
│  Search   │  │   Service       │    │   (Chat Completion)    │
│  Service  │  │  (MiniMax tool) │    │  + KP System Prompt    │
└─────┬─────┘  └────────┬────────┘    └──────────┬─────────────┘
      │                 │                         │
      ▼                 ▼                         ▼
┌───────────┐  ┌─────────────────┐    ┌────────────────────────┐
│ pgvector  │  │  Values Filter  │    │  Output Validator      │
│  (chunks) │  │  Service        │    │  (MiniMax lightweight) │
└───────────┘  └─────────────────┘    └────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                   INGESTION PIPELINE (Admin only)              │
│  POST /admin/ingest                                            │
│  DocumentParser → TextChunker → EmbeddingService → pgvector   │
│  (Apache PDFBox / POI) → (512 char chunks, 50 overlap)        │
│                           → (embo-01 embeddings)              │
└────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────┐
│                       SHARED INFRASTRUCTURE                    │
│  PostgreSQL + pgvector  │  MiniMaxClient (OkHttp3 wrapper)    │
│  Flyway migrations      │  Spring Data JPA repositories       │
└────────────────────────────────────────────────────────────────┘
```

---

## Ingestion Flow

1. Admin uploads file via `POST /admin/ingest` with `docType` param
2. `IngestionController` validates X-Admin-Key header
3. `DocumentParser` converts PDF/DOCX/TXT → clean plain text + extracts metadata
4. `TextChunker` splits text into ~400-char chunks with 50-char overlap, preserving sentence boundaries
5. `EmbeddingService` checks file hash — skips if already ingested (idempotency)
6. For each batch of 10 chunks: call `MiniMaxClient.embed()` (embo-01)
7. Store `(chunk_text, embedding vector, doc_id, chunk_index)` in pgvector
8. Return `IngestionResult(chunksCreated, tokensUsed, skipped, durationMs)`

---

## Query (Chat) Flow

1. User sends message via `POST /chat/message {sessionId, message}`
2. Rate limiter checks: 20 req/min per session (Bucket4j)
3. `ChatService` embeds query via `MiniMaxClient.embed()` (embo-01)
4. `VectorSearchService` runs cosine similarity search → top-5 chunks
5. `WebSearchService` calls MiniMax with web_search tool → up to 3 web results
6. `ValuesFilterService` calls MiniMax-Text-01 per web result → keep only PASS results
7. `ContextAssembler` merges: 60% doc chunks (~1800 chars) + 30% web (~900 chars)
8. `ChatService` calls `MiniMaxClient.chat()` with KP system prompt + context + last 10 messages
9. `OutputValidatorService` validates response for values alignment → fallback if FAIL
10. Save exchange to `messages` table, return `ChatMessageResponse(response, sources[])`

---

## Technology Decisions

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Runtime | Spring Boot 3 / Kotlin | KP existing stack; Kotlin null safety suits AI data flows |
| LLM (chat) | MiniMax-Text-01 | Free tier, strong Bahasa Indonesia, 1M context window |
| LLM (embeddings) | MiniMax embo-01 | Same API key; avoids second vendor |
| Vector DB | PostgreSQL + pgvector | Reuses existing PostgreSQL infra; no extra service cost |
| HTTP client | OkHttp3 | Kotlin-idiomatic; built-in retry interceptors |
| DB migrations | Flyway | Spring Boot native integration |
| Rate limiting | Bucket4j | Spring Boot native; no Redis dependency for MVP |
| Doc parsing | Apache PDFBox + POI | Java-native; no external service |

See `docs/adr/` for detailed decision rationale.

---

## API Surface

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/chat/session` | None | Create new conversation session |
| POST | `/chat/message` | None | Send message, get AI response |
| GET | `/chat/history/{sessionId}` | None | Retrieve conversation history |
| POST | `/admin/ingest` | X-Admin-Key | Upload single document for ingestion |
| POST | `/admin/ingest/batch` | X-Admin-Key | Upload multiple documents |
| GET | `/actuator/health` | None | Health check (Docker, load balancer) |

---

## Data Model

```
documents
  id UUID PK
  source VARCHAR        -- original filename
  doc_type VARCHAR      -- FAQ | TOS | BRAND | HOWTO
  file_hash VARCHAR     -- SHA-256 of file bytes (idempotency)
  created_at TIMESTAMP

chunks
  id UUID PK
  doc_id UUID FK → documents.id
  chunk_text TEXT
  chunk_index INT
  token_count INT
  embedding vector(1536)   -- pgvector column (embo-01 output)
  created_at TIMESTAMP
  INDEX: ivfflat on embedding for cosine distance

sessions
  id UUID PK
  created_at TIMESTAMP
  last_active_at TIMESTAMP

messages
  id UUID PK
  session_id UUID FK → sessions.id
  role VARCHAR          -- user | assistant
  content TEXT
  created_at TIMESTAMP
```

---

## External Dependencies

| Service | Purpose | Endpoint | Free Tier |
|---------|---------|---------|-----------|
| MiniMax API | Embeddings + chat + web search | api.minimax.chat | Yes |
| PostgreSQL + pgvector | Vector store + relational data | localhost:5432 (dev) | Self-hosted |

No other external dependencies for MVP.

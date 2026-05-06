# Spec: KP AI Chatbot MVP

> **Status:** Draft  
> **Author:** Product Owner  
> **Date:** 2026-05-06  
> **Related PRD:** [docs/prd/kp-ai-chatbot.md](../../docs/prd/kp-ai-chatbot.md)

---

## Overview

This specification defines the engineering deliverables for the KP AI Chatbot MVP, a RAG-based conversational assistant built on Spring Boot 3 / Kotlin and MiniMax APIs. It implements the product requirements documented in the linked PRD and must adhere to the constraints in `AGENTS.md` and `docs/architecture.md`.

---

## Scope

### In Scope
- REST API for chat sessions and messages (`/chat/session`, `/chat/message`, `/chat/history/{sessionId}`).
- Document ingestion pipeline (`/admin/ingest`, `/admin/ingest/batch`).
- Vector search (pgvector), web search (MiniMax tool), values filter, and output validator.
- Session-based conversation history (last 10 messages).
- Rate limiting (20 req/min/session via Bucket4j).
- Idempotent document ingestion (SHA-256 file hash deduplication).

### Out of Scope
- Core banking integration for real-time loan data.
- Consumer-facing chat widget (API-only).
- Multi-language beyond Bahasa Indonesia + English fallback.
- Voice interface.

---

## Technical Constraints

- Kotlin only; strict null safety (`!!` requires an explanatory comment).
- All MiniMax API calls must route through `MiniMaxClient.kt` (retry + rate-limit handling).
- Web search results must pass through `ValuesFilterService` before entering LLM context.
- Every response must pass through `OutputValidatorService` before being returned to the user.
- No Flyway migrations outside the dev Docker environment (`make dev-up`).
- No direct commits to `main`; changes require a PR.

---

## Dependencies

- MiniMax API (embeddings, chat completion, web search)
- PostgreSQL + pgvector
- Spring Boot 3, Spring Data JPA, Flyway
- Bucket4j (rate limiting)
- Apache PDFBox + Apache POI (document parsing)
- OkHttp3 (HTTP client via `MiniMaxClient`)

---

## Deliverables

1. Backend services implemented per `docs/architecture.md` component diagram.
2. Flyway migrations for `documents`, `chunks`, `sessions`, and `messages` tables.
3. Admin ingestion endpoints secured by `X-Admin-Key`.
4. Unit and integration tests with `./gradlew test` passing.
5. `ktlintCheck` clean.

---

## Acceptance Criteria

All Gherkin scenarios in [docs/prd/kp-ai-chatbot.md](../../docs/prd/kp-ai-chatbot.md) Section 7 must pass in the integration test suite before the spec is considered approved.

---

## Open Questions

See PRD Section 9. No engineering work should begin on items marked with open questions until the question is resolved and, if necessary, an ADR is created in `docs/adr/`.

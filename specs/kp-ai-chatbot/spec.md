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
#### Backend
- REST API for chat sessions and messages (`/chat/session`, `/chat/message`, `/chat/history/{sessionId}`).
- Document ingestion pipeline (`/admin/ingest`, `/admin/ingest/batch`).
- Vector search (pgvector), web search (MiniMax tool), values filter, and output validator.
- Session-based conversation history (last 10 messages).
- Rate limiting (20 req/min/session via Bucket4j).
- Idempotent document ingestion (SHA-256 file hash deduplication).

#### Frontend
- React/TypeScript consumer chat UI (`ChatPage`, `MessageList`, `MessageInput`, `TypingIndicator`, `SourceBadge`, `NewChatButton`).
- Responsive layout (320 px – 1920 px) using Tailwind CSS.
- WCAG 2.1 AA accessibility compliance (axe-core 0 violations).
- Rate-limit awareness in the UI (disable input, show BI warning).

### Out of Scope
- Core banking integration for real-time loan data.
- Multi-language beyond Bahasa Indonesia + English fallback.
- Voice interface.

---

## Technical Constraints

### Backend
- Kotlin only; strict null safety (`!!` requires an explanatory comment).
- All MiniMax API calls must route through `MiniMaxClient.kt` (retry + rate-limit handling).
- Web search results must pass through `ValuesFilterService` before entering LLM context.
- Every response must pass through `OutputValidatorService` before being returned to the user.
- No Flyway migrations outside the dev Docker environment (`make dev-up`).

### Frontend
- React 18+ with TypeScript; Tailwind CSS only — no inline styles.
- All API URLs via environment variable (`REACT_APP_API_URL`).
- Session tokens stored in `sessionStorage` only (not `localStorage`).
- All user-facing text in Bahasa Indonesia; map API errors to friendly BI messages.
- All interactive elements must have Indonesian `aria-label` attributes.
- No direct MiniMax API calls from the browser.
- No `console.log` in production builds.

### Shared
- No direct commits to `main`; changes require a PR.

---

## Dependencies

#### Backend
- MiniMax API (embeddings, chat completion, web search)
- PostgreSQL + pgvector
- Spring Boot 3, Spring Data JPA, Flyway
- Bucket4j (rate limiting)
- Apache PDFBox + Apache POI (document parsing)
- OkHttp3 (HTTP client via `MiniMaxClient`)

#### Frontend
- React 18+ / TypeScript
- Tailwind CSS
- React Testing Library + Playwright (testing)
- axe-core (accessibility audit)

---

## Deliverables

#### Backend
1. Backend services implemented per `docs/architecture.md` component diagram.
2. Flyway migrations for `documents`, `chunks`, `sessions`, and `messages` tables.
3. Admin ingestion endpoints secured by `X-Admin-Key`.
4. Unit and integration tests with `./gradlew test` passing.
5. `ktlintCheck` clean.

#### Frontend
6. `ChatPage` with `MessageList`, `MessageInput`, `TypingIndicator`, `SourceBadge`, and `NewChatButton` components.
7. Responsive layout verified on 320 px, 768 px, and 1920 px viewports.
8. React Testing Library component tests (behavior-first).
9. Playwright E2E test covering: open chat → send message → verify citation → start new chat.
10. axe-core accessibility audit with zero violations.
11. Screenshots / Playwright recordings attached to PR.

---

## Acceptance Criteria

All Gherkin scenarios in [docs/prd/kp-ai-chatbot.md](../../docs/prd/kp-ai-chatbot.md) Section 7 must pass in the integration test suite before the spec is considered approved.

---

## Open Questions

See PRD Section 9. No engineering work should begin on items marked with open questions until the question is resolved and, if necessary, an ADR is created in `docs/adr/`.

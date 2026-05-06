# KP AI Chatbot

AI-powered customer support chatbot for Kredit Pintar. Combines document
knowledge (RAG via pgvector) with live web search, filtered through KP's
lending values layer. Built on Spring Boot 3 / Kotlin + MiniMax API.

## Commands
- `./gradlew build` — compile + all checks
- `./gradlew test` — unit + integration tests (REQUIRED before any PR)
- `./gradlew bootRun` — start local server (port 8080)
- `./gradlew ktlintCheck` — lint (Kotlin)
- `make dev-up` — start pgvector + local DB (Docker)
- `make dev-down` — stop local DB
- `make dev-reset` — wipe and recreate local DB

## Package Structure
- `api/` — REST controllers (ChatController, IngestionController)
- `config/` — Spring config, system prompts, values filter prompts
- `domain/` — JPA entities + Spring Data repositories
- `ingestion/` — DocumentParser, TextChunker, EmbeddingService
- `pipeline/` — VectorSearchService, WebSearchService, ValuesFilterService, ContextAssembler
- `service/` — ChatService (orchestrator), SessionService, OutputValidatorService
- `resources/db/migration/` — Flyway migrations (V1–V4)

## Conventions
- Kotlin only, no Java. Strict null safety — no `!!` operator without a comment.
- Commits: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`).
- Branches: `<type>/<scope>-<slug>` (e.g. `feat/pipeline-values-filter`).
- See `CONTRIBUTING.md` for full PR checklist.

## NEVER
- **NEVER** run Flyway migrations outside the dev Docker environment (`make dev-up`)
- **NEVER** call MiniMax API directly — always via `MiniMaxClient.kt` (handles retry + rate limits)
- **NEVER** inject web search results into LLM context without passing through `ValuesFilterService`
- **NEVER** return a chat response without running `OutputValidatorService`
- **NEVER** change `KpSystemPrompt.kt` or `ValuesFilterPrompt.kt` without human review and approval
- **NEVER** commit files matching `*.env`, `*api-key*`, `application-prod.yml`
- **NEVER** push directly to `main` — branch protection enforced, PRs only
- **NEVER** give any agent or service prod DB write credentials

## When Unsure
Check `docs/adr/` first. If no ADR covers the question, create a proposal at
`docs/adr/NNNN-proposal-<topic>.md` instead of guessing.

## Architecture & Decisions
- Full system diagram: `docs/architecture.md`
- All ADRs: `docs/adr/`
- Subagent definitions: `.claude/agents/`

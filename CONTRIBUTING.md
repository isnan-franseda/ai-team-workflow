# Contributing to KP AI Chatbot

## Branch Naming

```
<type>/<scope>-<slug>
```

| Type | When |
|------|------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `chore` | Build, deps, config, refactor |
| `docs` | Documentation only |
| `test` | Tests only |

**Examples:**
- `feat/pipeline-values-filter`
- `fix/embedding-batch-retry`
- `docs/adr-0004-session-storage`
- `chore/upgrade-pgvector-driver`

## Commit Format (Conventional Commits)

```
<type>(<scope>): <short description>

[optional body — explain WHY, not WHAT]
[optional footer: Closes #task-id]
```

**Examples:**
```
feat(pipeline): add cosine similarity threshold to VectorSearchService

Previously returning top-K regardless of score. Adding minimum score
threshold (default 0.7) prevents low-relevance chunks from polluting context.

Closes KP-042
```

```
fix(ingestion): skip re-embedding docs with matching file hash

Idempotency was broken when doc filename changed but content was identical.
Now hashing file bytes, not filename.
```

## PR Checklist

Every PR must satisfy all items before requesting review:

### Code Quality
- [ ] `./gradlew test` passes (zero failures)
- [ ] `./gradlew ktlintCheck` passes (zero warnings)
- [ ] No `!!` (non-null assertion) without an explanatory comment
- [ ] No `println()` or `System.out` — SLF4J Logger only
- [ ] No hardcoded API keys, URLs, or credentials

### AI Chatbot Specific
- [ ] Values filter is called for any new web-search integration path
- [ ] Output validator is called for any new response generation path
- [ ] `KpSystemPrompt.kt` / `ValuesFilterPrompt.kt` unchanged OR human-approved
- [ ] Any new MiniMax API call goes through `MiniMaxClient.kt` (not raw HTTP)
- [ ] New Flyway migration tested against `make dev-reset` before PR

### Documentation
- [ ] PR description references task ID (e.g. `Closes KP-042`)
- [ ] If a new architectural decision was made, an ADR exists in `docs/adr/`
- [ ] `docs/architecture.md` updated if component diagram changed
- [ ] New public methods have KDoc comments

### Testing
- [ ] New business logic has unit tests
- [ ] Happy path + at least one failure/edge case tested
- [ ] Any new REST endpoint covered by an integration test

## Merge Requirements

- CI must be green (build + test + lint)
- At least 1 human reviewer approval
- No unresolved review comments
- Branch must be up to date with `main` (rebase, not merge)

## Adding an ADR

1. Copy `docs/adr/TEMPLATE.md` to `docs/adr/NNNN-<topic>.md` (next sequential number)
2. Fill in Context, Decision, Consequences
3. Reference the ADR in your PR description and in `AGENTS.md` if it affects agent behavior
4. Mark Status: `Accepted` when PR merges

## AI Agent Contributors

If you are an AI agent (Claude Code, Copilot Agent, etc.):
- Read `AGENTS.md` at repo root before making any changes
- Read the relevant `docs/adr/` files for your task's domain
- Open **draft PRs only** — never mark as Ready for Review without a human check
- If acceptance criteria are ambiguous: STOP, post a question on the Linear ticket, do not guess
- Max 1 PR per ticket. If scope grows unexpectedly, file a new ticket first.

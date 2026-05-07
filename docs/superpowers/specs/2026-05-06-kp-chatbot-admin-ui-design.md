# Design: KP AI Chatbot + Admin UI — Phased Parallel Execution

> **Status:** Approved  
> **Date:** 2026-05-06  
> **Related PRDs:** [kp-ai-chatbot.md](../prd/kp-ai-chatbot.md), [admin-document-upload-ui.md](../prd/admin-document-upload-ui.md)  
> **Related Specs:** [kp-ai-chatbot/spec.md](../specs/kp-ai-chatbot/spec.md), [admin-document-upload-ui/spec.md](../specs/admin-document-upload-ui/spec.md)

---

## Overview

This design document outlines the execution approach for delivering the KP AI Chatbot MVP and Admin Document Upload UI as interconnected features built by a 7-person team (2 backend, 2 frontend, 2 QA, 1 PM) in a 3-day timeline.

**Approach:** Phased Parallel Execution
- **Day 0:** API design spike + component scaffold (backend & frontend work independently)
- **Day 1-2:** Parallel implementation with API contract lock (both teams build simultaneously)
- **Day 2-3:** Integration testing & stabilization

**Key principle:** Minimize blocking dependencies by locking API contracts on Day 0, enabling frontend to build against mocks while backend implements the real API in parallel.

---

## Team Structure & Responsibilities

| Role | Count | Day 0-1 | Day 1-2 | Day 2-3 |
|------|-------|---------|---------|---------|
| **Backend Engineers** | 2 | API design + DB spike | Core services + endpoints | Integration fixes + perf validation |
| **Frontend Engineers** | 2 | Component scaffold + mocks | Real API integration | E2E testing + accessibility |
| **QA Engineers** | 2 | Test strategy + setup | Unit/integration tests (paired) | Full E2E validation + sign-off |
| **Product Manager** | 1 | Coordinate API contract | Unblock teams, sync daily | Triage blockers, prepare rollout |

---

## Execution Timeline

### Day 0-1: API Design & Spike Phase (~6-8 hours)

**Backend Team (6 hours):**
1. Design database schema: `documents`, `chunks`, `sessions`, `messages` tables (PostgreSQL + pgvector)
2. Define REST API contract (OpenAPI/Swagger):
   - `POST /chat/send` — request/response shape, error codes
   - `POST /admin/ingest` — single file upload
   - `POST /admin/ingest/batch` — multi-file upload
   - `GET /admin/documents` — document list + metadata
3. Set up Spring Boot 3 project scaffold with:
   - Flyway migrations
   - PostgreSQL + pgvector extension
   - Bucket4j rate limiter
4. Spike MiniMax client integration: validate API keys, test embedding call
5. Create API contract document (shared with frontend team)

**Deliverables:**
- Database schema diagram (ER diagram or SQL)
- OpenAPI/Swagger spec (`docs/openapi.yaml` or Postman collection)
- Working Flyway migrations
- Git commit with schema + client spike

**Frontend Team (2 hours):**
1. Review API contract document
2. Create Vite/React scaffold aligned with existing frontend project structure
3. Define component structure:
   - `UploadPage` — drag-and-drop, doc_type selector, progress, results (Bahasa Indonesia)
   - `DocumentList` — table view of ingested documents
   - `AdminKeyPrompt` — session-level key input
4. Create mock API client matching API contract (stub responses)
5. Build component skeleton (empty implementations, storybook if available)

**Deliverables:**
- React component skeleton
- Mock API client (`src/api/mockClient.ts`)
- Git commit with scaffold

**QA Team (3 hours):**
1. Review PRD + spec acceptance criteria
2. Create test strategy document:
   - Unit test coverage targets (backend services, frontend components)
   - Integration test scenarios (API contract validation)
   - E2E test scope (critical user journeys)
3. Set up test infrastructure:
   - Jest + React Testing Library (frontend)
   - Gradle + JUnit + Testcontainers (backend PostgreSQL)
4. Identify gaps early (missing endpoints, unclear error codes)

**Deliverables:**
- Test strategy document
- Test infrastructure ready (CI/CD hooks configured)
- Git commit with test setup

**Coordination Gate (EOD Day 0):**
- **API contract locked** — all field names, error codes, timing assumptions finalized
- **Both teams commit** to contract (frontend: mock client uses contract; backend: implements contract)
- Any post-lock changes require PM approval

---

### Day 1-2: Parallel Implementation Phase (~40 hours across team)

**Backend Team (~20 hours, 2 engineers):**

| Service | Responsibility |
|---------|-----------------|
| `ChatService` | Session management, message retention, context assembly |
| `IngestionService` | Document parsing (PDF, DOCX, TXT), chunking, embedding via MiniMax |
| `DocumentService` | CRUD for documents, list ingestion history |
| `SessionService` | Session lifecycle, cleanup, rate limiting |
| REST Endpoints | `/chat/send`, `/admin/ingest`, `/admin/ingest/batch`, `/admin/documents` |
| Middleware | `ValuesFilterService` (web search filtering), `OutputValidatorService` (response validation) |

**Implementation priorities:**
1. Implement `IngestionService` + `/admin/ingest` (core chatbot knowledge base)
2. Implement `ChatService` + `/chat/send` (core chatbot conversation)
3. Implement `/admin/ingest/batch` + `/admin/documents`
4. Add `ValuesFilterService` + `OutputValidatorService` guards
5. Write unit tests for each service (target: 80% code coverage)
6. Write integration tests for API endpoints (Testcontainers with real PostgreSQL)

**Key constraints:**
- All MiniMax API calls via `MiniMaxClient.kt` (retry + rate-limit handling)
- Every response validated by `OutputValidatorService` before return
- Strict null safety: `!!` requires explanatory comment
- Commit frequently; maintain API contract stability

**Deliverables:**
- All backend services implemented + tested
- API endpoints live and contract-compliant
- Integration tests passing
- Git commits (1-2 per service)

**Frontend Team (~20 hours, 2 engineers):**

| Component | Responsibility |
|-----------|-----------------|
| `UploadPage` | Drag-and-drop zone, doc_type selector, upload button, progress state, result display (Bahasa Indonesia) |
| `DocumentList` | Read-only table: filename, doc_type, upload date, status |
| `AdminKeyPrompt` | Session-level admin key input, `sessionStorage` storage, header injection |
| Error mapper | Translate HTTP errors → Bahasa Indonesia messages |
| API client | Swap mock → live API client by EOD Day 1 |

**Implementation timeline:**
- Day 1 (morning): Component unit tests + implementation (mock client)
- Day 1 (afternoon): Integrate real API client; identify any contract mismatches
- Day 2 (morning): Fix integration bugs; accessibility audit (axe-core)
- Day 2 (afternoon): Responsive design validation; prepare for E2E

**Key constraints:**
- React 18+ with TypeScript; Vite scaffold
- Tailwind CSS only — no inline styles, no CSS-in-JS
- All interactive elements have `aria-label` (Bahasa Indonesia)
- Admin key in `sessionStorage` only; never `localStorage`
- No `console.log` in production builds
- Map all API errors to friendly Bahasa Indonesia messages

**Deliverables:**
- All components implemented with Bahasa Indonesia UI
- Component unit tests (React Testing Library)
- Real API client integrated by EOD Day 1
- Git commits (1 per component)

**QA Team (~16 hours, 2 engineers paired with dev teams):**

**Backend pairing (~8 hours):**
- Write integration tests alongside backend engineers
- Validate API contract: all endpoints respond with correct shape, status codes, error messages
- Test error scenarios: invalid input, missing documents, rate limit hit
- Log blockers for Day 2 resolution

**Frontend pairing (~8 hours):**
- Write component unit tests (React Testing Library): upload flow, error states, accessibility
- Test upload form: doc_type selection, file validation, progress feedback
- Test document list: rendering, error states
- Begin E2E test harness (Playwright) scaffold

**Coordination Point (EOD Day 1):**
- Backend API goes live; frontend switches from mock client → real API client
- Any contract mismatches logged as Day 2 blockers
- Daily standup: 15 min sync on blockers, API changes, integration issues

---

### Day 2-3: Integration & QA Phase (~36 hours across team)

**Backend Team (~12 hours):**
1. Fix integration bugs logged by QA/frontend (API field mismatches, timing issues)
2. Performance validation:
   - p95 response time < 3 seconds (measure end-to-end: user message → validated response)
   - Rate limiter works under load (20 req/min per session)
3. Security audit:
   - No PII in logs (`chatbot.response`, `chatbot.values_filter`, etc. metrics do not leak user data)
   - Session data encrypted at rest
   - Admin key validation strict (400 if missing, 401 if invalid)
4. Run full test suite: `./gradlew test` passing, `ktlintCheck` clean
5. Prepare staging environment: migrations auto-run, monitoring dashboards ready

**Deliverables:**
- All integration bugs fixed
- Performance validated
- Test suite green
- Ready for staging deployment

**Frontend Team (~12 hours):**
1. Fix API integration issues:
   - Missing fields in responses (adjust UI data binding)
   - Error message mapping gaps (add missing error codes)
   - Timing assumptions (adjust progress timeouts)
2. Accessibility audit: axe-core scan, zero violations
3. Responsive design validation: test at 1280×720 (minimum), full-width (maximum)
4. Performance: measure upload time end-to-end, adjust progress UX if needed
5. Prepare for deployment: build optimization, environment variables set, staging test

**Deliverables:**
- All integration issues resolved
- Accessibility audit passed (axe-core)
- Responsive design validated
- Ready for staging deployment

**QA Team (~12 hours):**
1. Full E2E test execution (Playwright):
   - Happy path: login → upload PDF → see results → check document list
   - Error cases: invalid file type, duplicate file, API failure
   - Batch upload: multiple files with mixed results
2. Regression testing: verify all PRD acceptance criteria met
3. Load testing: concurrent uploads, rate limiting, p95 response times
4. Document test results + any known issues
5. **Sign-off:** MVP ready or blockers escalated to PM

**Deliverables:**
- E2E test suite passing
- Test results document
- Sign-off on MVP readiness

**Product Manager (Day 2-3):**
- Daily standup: 15 min sync on progress, blockers, trade-offs
- Triage Day 2 blockers: scope trade-offs if needed
- Prepare rollout strategy: dog-food phase (internal team), beta phase (100 users), general availability
- Coordinate final sign-off with QA

---

## Coordination & Risk Mitigation

### API Contract Management
- **Day 0 lock:** Final contract signed off by backend lead + PM (frontend reviews for feasibility)
- **Change process:** Any post-lock changes require PM approval + both leads sign-off
- **Drift prevention:** QA validates contract compliance during integration (Day 1-2)

### Daily Standups (15 min)
- **Time:** 10:00 AM (adjust to team timezone)
- **Attendees:** Tech leads (1 backend, 1 frontend, 1 QA), PM
- **Format:** 
  - Blockers (what's stuck, who owns resolution)
  - Milestones (what got done)
  - API changes (any contract deviations)
  - Tomorrow's focus (who's doing what)

### Escalation Path
- **Blocker during Day 1-2:** QA/PM flags to tech lead → PM + leads resolve within 2 hours
- **Blocker during Day 2-3:** PM decides scope trade-off (descope features, extend timeline, or accept risk)
- **Critical issue post-sign-off:** Rollback to Day 2 state; hot-fix in parallel with rollout prep

### Dependency Management
- Backend → Frontend: API contract (locked Day 0)
- Frontend → Backend: API usage feedback (logged Day 1-2)
- Both → QA: Code ready for testing (ongoing)
- QA → PM: Sign-off decision (EOD Day 2)

---

## Success Criteria (Exit Definitions)

### Day 0 (API Design Complete)
- ✅ Database schema finalized + Flyway migrations in Git
- ✅ OpenAPI/Swagger contract published
- ✅ Component skeleton in Git
- ✅ Test infrastructure ready

### Day 1 (Core Implementation & API Live)
- ✅ Backend API endpoints live + contract-compliant
- ✅ Frontend components integrated with real API
- ✅ Integration bugs identified (Day 2 work)
- ✅ Unit test coverage ≥ 80% (backend), ≥ 70% (frontend)

### Day 2-3 (Integration & QA Complete)
- ✅ E2E tests green
- ✅ All PRD acceptance criteria passing
- ✅ Performance validated (p95 < 3s, rate limiting works)
- ✅ Security audit passed (no PII in logs, session encryption verified)
- ✅ Accessibility audit passed (axe-core zero violations)
- ✅ QA sign-off: **MVP ready for staging/production**

---

## Out of Scope (for this 3-day sprint)

- Multi-language support beyond Bahasa Indonesia (English fallback only)
- Voice interface
- Consumer-facing chat widget (API-only for MVP)
- RBAC or multi-user admin access (single shared admin key)
- Document preview, editing, or deletion in UI
- Dark mode or extensive theming
- Real-time ingestion progress streaming (indeterminate progress bar only)

---

## Post-Launch Rollout (not part of 3-day sprint)

1. **Internal dog-food (1 week):** KP engineering + ops teams test, gather feedback
2. **Limited beta (2 weeks):** 100 KP active users, monitor CSAT + rejection rates
3. **General availability:** All eligible borrowers

---

## Appendix: Open Questions

| # | Question | Owner | Resolution Date |
|---|----------|-------|-----------------|
| 1 | Which document types (FAQ, TOS, BRAND, HOWTO) must be pre-loaded before launch? | Content / PO | Day 0 |
| 2 | What is the fallback behavior if MiniMax API is down? (cached response, safe fallback, or error?) | Backend + PO | Day 0 |
| 3 | Is `/admin/documents` endpoint required for MVP, or can it be deferred? | Frontend + Backend + PO | Day 0 |
| 4 | Should admin key be entered once per session or on every page load? | Security / Frontend | Day 0 |
| 5 | What is the maximum batch file count/size for `/admin/ingest/batch`? | Backend + PO | Day 0 |

---

**Approved by:** Product Owner (2026-05-06)  
**Next step:** Implementation plan breakdown (separate backend, frontend, and unified plans)

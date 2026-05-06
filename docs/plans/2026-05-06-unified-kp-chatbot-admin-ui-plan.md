# KP AI Chatbot + Admin UI — Unified Coordination Plan

> **For team leads:** This plan coordinates the 7-person team (2 backend, 2 frontend, 2 QA, 1 PM) across 3 days. Each discipline has its own detailed plan; this document ensures synchronization.

**Goal:** Deliver working MVP of chatbot API + admin upload UI with end-to-end testing and QA sign-off in 3 days.

**Approach:** Phased parallel execution with API contract lock on Day 0, enabling frontend to build with mocks while backend implements the real API in parallel.

**Key Metrics:**
- API contract locked EOD Day 0 (frontend → backend handoff)
- API live + frontend integration complete EOD Day 1 (integration checkpoint)
- QA sign-off on MVP readiness EOD Day 2-3 (launch readiness)

---

## Team Leads Checklist

### Day 0: Setup & API Contract Lock

#### Morning (Backend Lead)
- [ ] Initialize Spring Boot 3 project scaffold in Git
- [ ] Create database schema (ER diagram for team review)
- [ ] Define REST API contract (OpenAPI/Swagger format)
- [ ] Spike MiniMax client integration; commit working example

#### Morning (Frontend Lead)
- [ ] Review API contract document (30 min review + feedback)
- [ ] Initialize React/Vite scaffold (or verify existing structure)
- [ ] Create component skeleton structure
- [ ] Build mock API client matching contract

#### Morning (QA Lead)
- [ ] Review PRD acceptance criteria; map to test scenarios
- [ ] Set up test infrastructure (Jest, React Testing Library, Gradle)
- [ ] Create test strategy document
- [ ] Identify scope gaps early (missing endpoints, unclear error codes)

#### EOD Day 0: API Contract Finalization
- [ ] **PM organizes sync:** All leads sign off on API contract (no more changes post-lock)
- [ ] **Backend Lead:** Commits final API contract to Git (immutable reference)
- [ ] **Frontend Lead:** Confirms mock client matches contract exactly
- [ ] **QA Lead:** Confirms test strategy aligns with contract

**Gate:** API contract signed off. All three teams commit to implementing/testing against this spec.

---

### Day 1: Parallel Implementation & Integration Start

#### Daily Standup (10:00 AM, 15 min)
- **Backend Lead:** "Core services done, API endpoints live by EOD today"
- **Frontend Lead:** "Components done with mock client; integrating real API by EOD"
- **QA Lead:** "Unit tests running alongside dev teams; integration bugs logged"
- **PM:** Unblock any team; flag scope changes

#### Afternoon (EOD Day 1): API Live Checkpoint
- [ ] **Backend Lead:** Confirms all endpoints live + passing integration tests
- [ ] **Frontend Lead:** Swaps mock client → real API client; logs integration mismatches
- [ ] **QA Lead:** Validates API contract compliance via integration tests
- [ ] **PM:** Compiles list of Day 2 blockers (if any)

**Gate:** API contract validated. Frontend integrated. Integration gaps logged for Day 2.

---

### Day 2-3: Integration Stabilization & QA Sign-Off

#### Daily Standup (10:00 AM, 15 min)
- **Backend Lead:** "Fixing Day 2 blockers; perf validation in progress"
- **Frontend Lead:** "Accessibility audit running; E2E tests prepared"
- **QA Lead:** "Full E2E test suite running; sign-off assessment in progress"
- **PM:** Make trade-off calls if scope slip necessary

#### EOD Day 2: Stability Checkpoint
- [ ] **Backend Lead:** All integration bugs fixed; test suite green
- [ ] **Frontend Lead:** Accessibility audit passed; responsive design validated
- [ ] **QA Lead:** E2E tests 80%+ passing; known issues documented
- [ ] **PM:** Assess sign-off readiness; escalate any blockers

#### EOD Day 3: Launch Readiness
- [ ] **QA Lead:** E2E tests 100% passing; sign-off approved OR blockers escalated
- [ ] **Backend Lead:** Staging environment ready; migrations auto-run verified
- [ ] **Frontend Lead:** Staging deployment ready; environment variables set
- [ ] **PM:** Prepare rollout comms (dog-food → beta → GA phases)

**Gate:** MVP ready for staging/production OR scope reduction agreed.

---

## Communication Protocol

### Blocker Escalation
**If a team is blocked by another:**
1. Tech lead flags in standup (within 2 hours of discovery)
2. PM organizes 15-min sync with both leads
3. Resolution committed within 2 hours (code change, contract clarification, scope trade-off)

### API Contract Changes Post-Lock
**If backend needs to change API after lock:**
1. Backend lead flags to PM + frontend lead within 1 hour
2. Frontend lead assesses impact (rework estimate)
3. PM decides: absorb change, descope, or extend timeline
4. All leads sign off before implementation

### Integration Issue Logging
**Daily sync (EOD each day):**
- Backend: log any API misunderstandings from frontend
- Frontend: log any API contract gaps or mismatches
- QA: log any test failures + probable cause (API issue vs. frontend issue)
- PM: triage for next-day priorities

---

## Detailed Plans

See the following for task-by-task breakdown:
- **Backend Implementation Plan:** `docs/plans/2026-05-06-backend-kp-chatbot-plan.md`
- **Frontend Implementation Plan:** `docs/plans/2026-05-06-frontend-admin-ui-plan.md`
- **QA Implementation Plan:** `docs/plans/2026-05-06-qa-testing-plan.md`

---

## Success Criteria (Exit Gates)

### Day 0 ✅
- Database schema committed + Flyway migrations working
- API contract (OpenAPI/Swagger) published + signed off
- React scaffold + mock client committed
- Test infrastructure ready (test commands executable)

### Day 1 ✅
- All backend API endpoints live + contract-compliant
- Frontend components integrated with real API (mock → live client swap)
- Unit tests ≥80% passing (backend), ≥70% passing (frontend)
- Integration test suite running; blockers logged

### Day 2 ✅
- All Day 1 blockers fixed
- Performance validated (p95 < 3s, rate limiting works)
- Accessibility audit passed (axe-core zero violations)
- Responsive design validated (1280×720 to full-width)

### Day 3 ✅
- E2E test suite 100% passing
- All PRD acceptance criteria verified
- Security audit passed (no PII in logs, session encryption confirmed)
- **QA sign-off: MVP ready for staging**

---

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|-----------|
| API contract changes after lock | Medium | High | PM approval gate + rework estimate before proceeding |
| Backend delayed → frontend blocked | Medium | High | Frontend uses mocks until API live; integration bugs logged for Day 2 |
| Integration bugs pile up Day 1-2 | Medium | High | QA paired with dev teams; bugs triaged daily; Day 2 blocker sync |
| Performance fails Day 2 | Low | High | Performance validation starts EOD Day 1; optimizations Day 2 morning |
| Scope creep | High | High | PM trade-off gate: descope features, accept risk, or extend timeline |

---

## Post-MVP Rollout (Out of Scope for This Sprint)

1. **Dog-food phase (1 week):** Internal team testing, monitoring CSAT + error rates
2. **Limited beta (2 weeks):** 100 KP users, gather feedback
3. **General availability:** All eligible borrowers

---

**Next Steps:**
1. Review backend plan: `docs/plans/2026-05-06-backend-kp-chatbot-plan.md`
2. Review frontend plan: `docs/plans/2026-05-06-frontend-admin-ui-plan.md`
3. Review QA plan: `docs/plans/2026-05-06-qa-testing-plan.md`
4. Team leads sync on communication protocol, escalation path, daily standup time
5. Engineer execution begins Day 0 morning

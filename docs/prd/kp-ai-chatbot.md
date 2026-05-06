# PRD: KP AI Chatbot MVP

> **Status:** Draft  
> **Author:** Product Owner  
> **Date:** 2026-05-06  
> **Related ADRs:** *None yet — ADR directory to be created as decisions are ratified*

---

## 1. Problem & Users

**Who has this problem?**
Active and prospective Kredit Pintar (KP) borrowers who need quick, accurate information about loan products, terms, limits, and repayment policies — often outside of normal customer-service hours.

**What is broken or missing today?**
- Borrowers must open a support ticket or call a hotline for routine questions, leading to long wait times and inconsistent answers.
- There is no self-service channel that reflects KP’s "financial partner" positioning; the existing experience feels transactional and reinforces the stigma of emergency lending.
- Support agents spend a disproportionate amount of time answering repetitive FAQs, increasing operational cost.

**Frequency & impact:**
- FAQ-style inquiries represent a high volume of incoming support traffic (exact percentage TBD — see Open Questions).
- Each delayed or inconsistent answer increases churn risk and reduces repeat borrowing, directly countering the "Grow the Loan Book" pillar.
- Poor support experience reinforces the perception of KP as an emergency lender, undermining the "Shift Stigma" pillar.

---

## 2. Goals & Non-Goals

**Goals:**
- Provide a 24/7 conversational interface that answers borrower questions instantly in Bahasa Indonesia.
- Ground every answer in KP’s official documents (FAQs, T&Cs, brand guidelines) and cite sources transparently to build trust.
- Filter all external information through KP’s lending-values layer so responses align with the "financial partner" brand.
- Reduce repetitive support-ticket volume by deflecting routine queries to the chatbot.
- Maintain conversation context across a session so users do not have to repeat themselves.

**Non-Goals:**
- Processing new loan applications or making credit decisions.
- Returning real-time individual loan balances or approval status from KP core banking.
- Multi-language support beyond Bahasa Indonesia (English fallback only for unrecognized queries).
- Voice-based interaction or a consumer-facing chat-widget UI (MVP is API-only).
- Using urgency tactics, fear-based messaging, or competitor comparisons to drive engagement.

---

## 3. Success Metrics

**Primary KPI:**
> Chatbot CSAT score ≥ 4.2 / 5 within 30 days of full rollout *(estimated — needs validation against industry benchmark)*.

**Guardrail metrics (must not regress):**
- Values-filter rejection rate < 5%.
- Output-validator fallback rate < 2%.
- p95 response time < 3 seconds.
- Support-ticket deflection rate for FAQ queries > 30% *(estimated — needs validation)*.
- Zero regulatory complaints arising from chatbot promises about loan approval.

---

## 4. User Stories (MoSCoW)

**MUST:**
- As a KP borrower, I want to ask about my loan limit so that I can plan my finances.
- As a KP borrower, I want to get a response in Bahasa Indonesia so that I understand the answer clearly.
- As a KP borrower, I want to see the source of the information so that I can trust the answer.
- As a KP borrower, I want the chatbot to remember what I already said in this conversation so that I do not have to repeat context.

**SHOULD:**
- As a KP borrower, I want answers that include up-to-date public information (e.g., regulatory changes) so that I am not relying solely on static documents.
- As a KP support lead, I want rejected web-search results and failed output validations to be logged so that I can review them for model improvement.

**COULD:**
- As a KP borrower, I want to start a new conversation while keeping access to old ones so that I can manage multiple topics over time.
- As a KP content manager, I want to upload new FAQ documents via an admin endpoint so that the chatbot knowledge stays current without a code deployment.

---

## 5. Functional Requirements

| ID | Requirement |
|----|------------|
| FR-01 | The chatbot MUST respond in Bahasa Indonesia for all queries that are recognized as Indonesian. |
| FR-02 | The chatbot MUST cite the document source (doc_type and source filename) when using KP document knowledge. |
| FR-03 | The chatbot MUST NOT make promises about loan approval, specific loan amounts, or guaranteed credit decisions. |
| FR-04 | The chatbot MUST pass any web-search result through the Values Filter before injecting it into the LLM context. |
| FR-05 | The chatbot MUST run the Output Validator on every generated response before returning it to the user. |
| FR-06 | The chatbot MUST retain the last 10 messages of the current session as conversation history for context assembly. |
| FR-07 | The chatbot MUST enforce a rate limit of 20 requests per minute per session. |
| FR-08 | The admin ingestion endpoint MUST accept PDF, DOCX, and TXT files, chunk them, embed them, and store them in pgvector idempotently (by file hash). |
| FR-09 | If the Output Validator marks a response as FAIL, the system MUST return a safe fallback message and log the failure for human review. |
| FR-10 | If no relevant document chunks are found, the chatbot MAY fall back to web search, subject to FR-04. |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|------------|
| Performance | p95 response time < 3 seconds (end-to-end: user message → validated response). |
| Availability | 99.5% uptime during business hours (07:00–22:00 WIB). |
| Rate limiting | 20 requests/minute per session (Bucket4j). |
| Security | No PII stored in application logs; session data encrypted at rest. |
| Language | Bahasa Indonesia primary; English fallback for unrecognized queries. |
| Accessibility | N/A (API-only for MVP). |
| Scalability | Ingestion pipeline must handle documents up to 50 MB without blocking the chat API. |

---

## 7. Acceptance Criteria (Gherkin)

```gherkin
Feature: KP Chatbot Response

  Scenario: FAQ query answered from document knowledge
    Given a user has an active session
    And KP FAQ documents are ingested
    When the user asks "Berapa limit pinjaman maksimal saya?"
    Then the chatbot responds in Bahasa Indonesia
    And the response includes a citation from the FAQ document
    And the response time is under 3 seconds
    And the response does not promise a specific loan amount

  Scenario: Web search result fails values filter
    Given a web search result contains competitor promotion
    When the values filter evaluates the result
    Then the result is marked FAIL
    And the result is NOT injected into the LLM context
    And the failure is logged with session ID

  Scenario: Output validator flags misleading response
    Given the LLM generates a response promising loan approval
    When the output validator evaluates the response
    Then the response is marked FAIL
    And the safe fallback message is returned to the user
    And the failure is logged for human review

  Scenario: Session continuity across multiple turns
    Given a user has an active session with 3 previous exchanges
    When the user asks "Bagaimana cara mengubahnya?" (referring to the previous topic)
    Then the chatbot understands the anaphora
    And responds with context from the earlier part of the conversation
    And includes the prior document citations where relevant
```

---

## 8. Out of Scope

- Multi-language support beyond Bahasa Indonesia + English fallback — future phase.
- Voice interface.
- Integration with KP core banking system (read individual loan data directly).
- Consumer chat-widget UI (API only for MVP).
- Real-time payment or disbursement actions.
- Agent hand-off / live-chat escalation.
- A/B testing of system prompts without human PO review.

---

## 9. Open Questions

| # | Question | Owner | Due |
|---|---------|-------|-----|
| 1 | Which document types must be ingested before launch, and who validates accuracy? | Content / PO | — |
| 2 | What is the exact fallback behavior if MiniMax API is down or rate-limits are hit? | Backend team | — |
| 3 | Who monitors the values-filter rejection log on a daily/weekly basis? | Product / Compliance | — |
| 4 | What is the source for the CSAT ≥ 4.2 target and deflection-rate benchmark? | Product / Data | — |
| 5 | Do we need a formal ADR for the choice of MiniMax over alternative LLM providers? | Architect | — |

---

## 10. Rollout & Telemetry

**Rollout plan:**
1. Internal dog-food (KP engineering and operations teams, 1 week).
2. Limited beta (100 KP active users, 2 weeks).
3. Full rollout to all eligible borrowers.

**Telemetry to instrument:**
- `chatbot.response.time_ms` (histogram)
- `chatbot.values_filter.result` (pass/fail count by doc_type)
- `chatbot.output_validator.result` (pass/fail count)
- `chatbot.session.messages_per_session` (gauge)
- `chatbot.ingestion.chunks_created` (counter)
- `chatbot.source.used` (count by source type: document vs web vs fallback)

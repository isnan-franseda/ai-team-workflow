# PRD: <Feature Name>

> **Status:** Draft | Under Review | Approved  
> **Author:** <PO name>  
> **Date:** YYYY-MM-DD  
> **Related ADRs:** docs/adr/NNNN-*.md

---

## 1. Problem & Users

**Who has this problem?**
<Describe the user segment — e.g., KP borrowers, KP support agents>

**What is broken or missing today?**
<Describe the current state and its pain points with evidence>

**Frequency & impact:**
<How often does this problem occur? What is the business or user cost?>

---

## 2. Goals & Non-Goals

**Goals:**
- Goal 1: ...
- Goal 2: ...

**Non-Goals:**
- Non-goal 1: ...
- Non-goal 2: ...

---

## 3. Success Metrics

**Primary KPI:**
> <e.g., "Chatbot CSAT score ≥ 4.2/5 within 30 days of launch">

**Guardrail metrics (must not regress):**
- <e.g., "Values filter rejection rate < 5%">
- <e.g., "Output validator fallback rate < 2%">
- <e.g., "p95 response time < 3s">

---

## 4. User Stories (MoSCoW)

**MUST:**
- As a KP borrower, I want to ask about my loan limit so that I can plan my finances.
- As a KP borrower, I want to get a response in Bahasa Indonesia so that I understand the answer clearly.

**SHOULD:**
- As a KP borrower, I want to see the source of the information so that I can trust the answer.

**COULD:**
- As a KP borrower, I want to continue a previous conversation so that I don't have to repeat context.

---

## 5. Functional Requirements

| ID | Requirement |
|----|------------|
| FR-01 | The chatbot MUST respond in Bahasa Indonesia |
| FR-02 | The chatbot MUST cite the document source when using KP document knowledge |
| FR-03 | The chatbot MUST NOT make promises about loan approval |
| FR-04 | The chatbot MUST use the values filter before injecting any web content |
| FR-05 | <add more> |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|------------|
| Performance | p95 response time < 3 seconds |
| Availability | 99.5% uptime during business hours (7am–10pm WIB) |
| Rate limiting | 20 requests/minute per session |
| Security | No PII stored in logs; session data encrypted at rest |
| Language | Bahasa Indonesia primary; fallback English |
| Accessibility | N/A (API-only for MVP) |

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
```

---

## 8. Out of Scope

- Multi-language support (non-Bahasa Indonesia) — future phase
- Voice interface
- Integration with KP core banking system (read loan data directly)
- Chat widget UI (API only for MVP)

---

## 9. Open Questions

| # | Question | Owner | Due |
|---|---------|-------|-----|
| 1 | Which document types must be ingested before launch? | Frans | - |
| 2 | What is the fallback behavior if MiniMax API is down? | Backend team | - |
| 3 | Who monitors the values filter rejection log? | Product | - |

---

## 10. Rollout & Telemetry

**Rollout plan:**
1. Internal dog-food (KP engineering team, 1 week)
2. Limited beta (100 KP users, 2 weeks)
3. Full rollout

**Telemetry to instrument:**
- `chatbot.response.time_ms` (histogram)
- `chatbot.values_filter.result` (pass/fail count by doc_type)
- `chatbot.output_validator.result` (pass/fail count)
- `chatbot.session.messages_per_session` (gauge)
- `chatbot.ingestion.chunks_created` (counter)

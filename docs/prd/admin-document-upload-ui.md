# PRD: Admin Document Upload UI

> **Status:** Draft  
> **Author:** Product Owner  
> **Date:** 2026-05-06  
> **Related PRD:** [kp-ai-chatbot.md](./kp-ai-chatbot.md)  
> **Related ADRs:** *None yet*

---

## 1. Problem & Users

**Who has this problem?**
KP operations and content teams who are responsible for keeping the chatbot knowledge base accurate and up to date.

**What is broken or missing today?**
- Document ingestion is only exposed as REST endpoints (`/admin/ingest`, `/admin/ingest/batch`) requiring API client tools (e.g., cURL, Postman).
- Non-technical ops staff cannot easily upload FAQ documents, T&Cs, or brand guidelines without engineering support.
- There is no visibility into which documents have been ingested, how many chunks were created, or whether a previous upload was skipped due to idempotency (file hash duplicate).
- Mistakes in `doc_type` selection or file uploads are only visible through raw JSON responses.

**Frequency & impact:**
- Content updates are expected weekly to monthly (new FAQs, policy changes, campaign materials).
- Each upload currently requires an engineer to run the request, creating a bottleneck and delaying knowledge-base refreshes.
- Slow updates lead to stale chatbot answers, reducing user trust and increasing support tickets.

---

## 2. Goals & Non-Goals

**Goals:**
- Provide a secure, web-based admin UI that allows ops team members to upload documents to the chatbot knowledge base without engineering assistance.
- Support both single-file and batch (multi-file) uploads.
- Display clear feedback on ingestion results: chunks created, tokens used, skipped duplicates, and errors.
- Show a read-only list of previously ingested documents with metadata (filename, doc_type, upload date, status).
- Ensure all user-facing text is in Bahasa Indonesia.
- Prevent accidental re-upload of identical files via visible idempotency indicators.

**Non-Goals:**
- In-app document editing or chunk-level manipulation (re-ingest requires deleting and re-uploading at the document level).
- Real-time collaboration or simultaneous multi-user editing.
- Role-based access control beyond a single admin passkey (RBAC is post-MVP).
- Public-facing access — this UI is strictly for internal ops users.
- Direct deletion of vector chunks from the UI (deferred to backend admin tooling for MVP).

---

## 3. Success Metrics

**Primary KPI:**
> 100% of knowledge-base document uploads are performed by ops team members via the UI without engineering involvement within 2 weeks of launch *(estimated — needs validation via usage analytics)*.

**Guardrail metrics (must not regress):**
- Ingestion API error rate < 2% (uploads that reach the backend but fail parsing/embedding).
- p95 upload-to-feedback time < 10 seconds for files ≤ 10 MB.
- Zero leaked admin credentials or session tokens in browser storage.
- UI accessibility violations = 0 (axe-core automated check).

---

## 4. User Stories (MoSCoW)

**MUST:**
- As an ops team member, I want to drag and drop a PDF/DOCX/TXT file into a web page so that I can ingest it into the knowledge base without using curl.
- As an ops team member, I want to select a `doc_type` (FAQ | TOS | BRAND | HOWTO) for each upload so that the chatbot categorizes the knowledge correctly.
- As an ops team member, I want to see the ingestion result (chunks created, tokens used, skipped, duration) immediately after upload so that I know whether it succeeded.
- As an ops team member, I want to view a list of all previously ingested documents with their type and upload date so that I can verify what is in the knowledge base.
- As an ops team member, I want all UI text to be in Bahasa Indonesia so that I can use the tool confidently.

**SHOULD:**
- As an ops team member, I want to upload multiple files at once (batch) so that I can perform bulk updates efficiently.
- As an ops team member, I want to see a warning if a file was skipped because it was already ingested (same hash) so that I do not accidentally think the update failed.

**COULD:**
- As an ops team member, I want to search or filter the document list by `doc_type` or filename so that I can quickly find a specific document.
- As an ops team member, I want to download the original uploaded file from the document list so that I can verify what version is in the system.

---

## 5. Functional Requirements

| ID | Requirement |
|----|------------|
| FR-01 | The UI MUST provide a drag-and-drop zone for file selection (single and multi-file). |
| FR-02 | The UI MUST restrict file types to PDF, DOCX, and TXT before sending to the API. |
| FR-03 | The UI MUST require the user to select a `doc_type` (FAQ / TOS / BRAND / HOWTO) before enabling upload. |
| FR-04 | The UI MUST display ingestion results in Bahasa Indonesia, including: jumlah chunk, token yang digunakan, file yang dilewati (jika duplikat), and lama proses. |
| FR-05 | The UI MUST show a read-only table/list of ingested documents with columns: nama file, jenis dokumen, tanggal unggah, and status. |
| FR-06 | The UI MUST authenticate requests using the `X-Admin-Key` header without exposing the key in URLs or browser history. |
| FR-07 | The UI MUST map all API error responses to friendly Bahasa Indonesia messages (no raw stack traces or HTTP status codes shown to users). |
| FR-08 | The UI MUST show an indeterminate progress indicator during upload and ingestion, and a clear success or error state afterward. |
| FR-09 | The UI MUST be responsive down to 1280×720 resolution for ops-team laptops. |
| FR-10 | The UI MUST use Tailwind CSS for styling and contain no inline styles. |

---

## 6. Non-Functional Requirements

| Category | Requirement |
|----------|------------|
| Performance | p95 time from clicking “Unggah” to seeing results < 10 seconds for files ≤ 10 MB. |
| Security | Admin key is stored in `sessionStorage` only (not `localStorage`) and cleared on tab close. |
| Security | No PII or file content is logged to the browser console. |
| Accessibility | All buttons, inputs, and modals have Indonesian `aria-label` attributes; axe-core score 0 violations. |
| Language | All user-facing text in Bahasa Indonesia; error messages mapped from generic HTTP errors to friendly BI text. |
| Compatibility | Chrome, Firefox, Safari (latest 2 versions). |

---

## 7. Acceptance Criteria (Gherkin)

```gherkin
Feature: Admin Document Upload UI

  Scenario: Single file upload and ingestion feedback
    Given an ops user is on the admin upload page
    And the user has entered a valid admin key
    When the user drags a 2 MB PDF into the drop zone
    And selects “FAQ” as the doc type
    And clicks “Unggah Dokumen”
    Then a progress indicator appears
    And within 10 seconds the user sees a success message
    And the result shows the number of chunks created and tokens used
    And the document appears in the “Dokumen Teringest” list

  Scenario: Duplicate file skipped with clear warning
    Given an ops user is on the admin upload page
    And the file “faq-mei-2026.pdf” was already ingested
    When the user uploads the exact same file again with the same hash
    Then the result shows a warning: “File ini sudah pernah diunggah. Tidak ada perubahan yang dilakukan.”
    And no duplicate chunks are created

  Scenario: Batch upload with mixed results
    Given an ops user is on the admin upload page
    When the user selects 3 files (2 new, 1 duplicate) and sets their doc types
    And clicks “Unggah Semua”
    Then the UI shows individual results for each file
    And the 2 new files show success with chunk counts
    And the duplicate file shows the skipped warning

  Scenario: API error mapped to friendly message
    Given an ops user is on the admin upload page
    And the backend ingestion endpoint returns a 500 error
    When the user uploads a valid file
    Then the UI does NOT show a raw stack trace or “500 Internal Server Error”
    And the user sees: “Terjadi kesalahan saat mengunggah. Silakan coba lagi atau hubungi tim engineering.”
```

---

## 8. Out of Scope

- Role-based access control (RBAC) or user management (single shared admin key for MVP).
- In-browser document preview or chunk-level editing.
- Deleting documents or chunks from the UI (requires backend admin tool for MVP).
- Integration with cloud storage (Google Drive, S3) for file picking.
- Dark mode or extensive theming (standard Tailwind light theme).
- Real-time ingestion progress streaming (progress bar is indeterminate for MVP).
- Audit log UI for who uploaded what (logged server-side only).

---

## 9. Open Questions

| # | Question | Owner | Due |
|---|---------|-------|-----|
| 1 | Should the admin key be entered once per session or on every page load? | Security / Frontend | — |
| 2 | Is there a design mockup or Figma file for the ops dashboard? | Design / PO | — |
| 3 | Do we need a separate `/admin/documents` list endpoint, or can we query existing tables directly via a new endpoint? | Backend | — |
| 4 | What is the maximum total batch size (number of files or combined MB) we want to support in one batch upload? | Backend / PO | — |
| 5 | Should the UI expose the SHA-256 hash to ops users for troubleshooting? | Product | — |

---

## 10. Rollout & Telemetry

**Rollout plan:**
1. Internal ops-team preview (3–5 users, 1 week) — gather usability feedback.
2. Limited rollout to full ops team (2 weeks).
3. General availability for all authorized admin users.

**Telemetry to instrument:**
- `admin_ui.upload.clicked` (counter)
- `admin_ui.upload.success` (counter by doc_type)
- `admin_ui.upload.skipped` (counter — duplicate hash)
- `admin_ui.upload.error` (counter by error category)
- `admin_ui.upload.duration_ms` (histogram — client-side from click to result render)
- `admin_ui.documents_list.viewed` (counter)

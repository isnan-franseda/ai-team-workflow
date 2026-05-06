# Spec: Admin Document Upload UI

> **Status:** Draft  
> **Author:** Product Owner  
> **Date:** 2026-05-06  
> **Related PRD:** [docs/prd/admin-document-upload-ui.md](../../docs/prd/admin-document-upload-ui.md)

---

## Overview

This specification defines the frontend engineering deliverables for the Admin Document Upload UI — an internal React/TypeScript web application that allows KP operations team members to upload and manage knowledge-base documents for the chatbot without engineering support.

---

## Scope

### In Scope
- React/TypeScript single-page admin dashboard.
- Drag-and-drop file upload component (single + batch).
- `doc_type` selector (FAQ | TOS | BRAND | HOWTO).
- Upload progress and result feedback in Bahasa Indonesia.
- Read-only document list table showing previously ingested files.
- Admin key input stored in `sessionStorage` and sent as `X-Admin-Key` header.
- Friendly error-message mapping for all API failures.
- Responsive layout (minimum 1280×720).
- axe-core accessibility compliance.

### Out of Scope
- RBAC or multi-user login.
- Document preview, editing, or deletion in the UI.
- Cloud-storage integrations.
- Real-time ingestion progress streaming.
- Dark mode.

---

## Technical Constraints

- React 18+ with TypeScript; Vite or Create React App (align with existing frontend package).
- Tailwind CSS only — no inline styles, no CSS-in-JS.
- All API URLs via environment variable (`REACT_APP_API_URL`).
- Admin key in `sessionStorage` only; never `localStorage`.
- No `console.log` in production builds.
- All interactive elements must have `aria-label` in Bahasa Indonesia.
- No direct MiniMax API calls from the browser.
- Map all API errors to friendly Bahasa Indonesia messages.

---

## API Dependencies

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/admin/ingest` | Single file upload + ingestion |
| POST | `/admin/ingest/batch` | Multi-file batch upload |
| GET | `/admin/documents` | *TBD — list ingested documents (Open Question #3)* |

> **Note:** If `/admin/documents` does not exist, escalate per frontend-agent constraints before implementing the list view.

---

## Deliverables

1. `UploadPage` component with drag-and-drop zone and `doc_type` selector.
2. `DocumentList` component showing ingestion history.
3. `AdminKeyPrompt` component for session-level authentication.
4. React Testing Library unit tests for all components (behavior-first).
5. Playwright E2E test covering: login → upload → verify result → check list.
6. axe-core accessibility audit with zero violations.
7. Screenshots / Playwright recordings attached to PR.

---

## Acceptance Criteria

All Gherkin scenarios in [docs/prd/admin-document-upload-ui.md](../../docs/prd/admin-document-upload-ui.md) Section 7 must pass in the E2E test suite before the spec is considered approved.

---

## Open Questions

See PRD Section 9. No frontend work should begin on items depending on open questions until they are resolved and, if necessary, a backend endpoint or ADR is delivered.

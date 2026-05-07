# Implementation Plan: Admin Document Upload UI

> **Status:** Draft
> **Date:** 2026-05-06
> **Related PRD:** [docs/prd/admin-document-upload-ui.md](../prd/admin-document-upload-ui.md)
> **Related Spec:** [specs/admin-document-upload-ui/spec.md](../../specs/admin-document-upload-ui/spec.md)
> **Agent:** frontend-agent (`.claude/agents/frontend-agent.md`)

---

## 1. Context

Greenfield React 18 / TypeScript single-page admin dashboard for KP operations team to upload and manage chatbot knowledge-base documents. Internal-only tool, no public-facing component.

**Zero frontend code exists.** A new `frontend/` directory at repo root will be created.

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Vite over CRA | CRA deprecated; Vite is React community standard |
| Tailwind CSS only | Hard constraint from spec; no CSS-in-JS, no inline styles |
| `sessionStorage` for admin key | Hard constraint; cleared on tab close |
| `VITE_API_URL` env var | All API calls through single `apiClient` module |
| Bahasa Indonesia UI | All labels, buttons, errors, aria-labels in BI |
| `X-Admin-Key` header auth | Injected by API client; never in URL |

### Open Questions (from PRD Section 9)

| # | Question | Impact |
|---|---------|--------|
| 1 | Admin key: once per session or every page load? | Plan assumes once-per-session via `AdminKeyPrompt`. If per-page needed, refactor to route gating wrapper. |
| 3 | Does `GET /admin/documents` exist? | Plan includes fallback: cache upload responses from session. Escalate if endpoint missing. |

---

## 2. Project Structure

```
frontend/
  public/
    favicon.ico
  src/
    api/
      apiClient.ts              # axios instance, interceptors, error mapping
      adminApi.ts               # POST /admin/ingest, /ingest/batch, GET /admin/documents
    components/
      AdminKeyPrompt/
        AdminKeyPrompt.tsx
        AdminKeyPrompt.test.tsx
      UploadPage/
        UploadPage.tsx
        DropZone.tsx
        DocTypeSelector.tsx
        UploadResultCard.tsx
        UploadPage.test.tsx
      DocumentList/
        DocumentList.tsx
        DocumentList.test.tsx
      shared/
        ProgressIndicator.tsx
        ErrorMessage.tsx
        EmptyState.tsx
    hooks/
      useSessionKey.ts          # sessionStorage read/write/clear
    i18n/
      messages.ts               # all BI strings
    types/
      api.ts                    # request/response types
      document.ts               # Document, DocType, IngestionResult, UploadStatus
    utils/
      fileValidation.ts         # MIME + extension checks
      errorMapper.ts            # HTTP → BI message mapping
    App.tsx
    main.tsx
    index.css                   # Tailwind directives ONLY
  .env                          # VITE_API_URL (committed placeholder)
  .env.example
  index.html
  package.json
  tsconfig.json
  vite.config.ts
  tailwind.config.js
  postcss.config.js
  playwright.config.ts
  e2e/
    admin-upload.spec.ts
```

---

## 3. Implementation Steps

### Step 1: Scaffold Vite + React + TypeScript

**Actions:**
1. `npm create vite@latest frontend -- --template react-ts`
2. Install: `tailwindcss`, `postcss`, `autoprefixer`, `axios`, `react-router-dom`
3. Install dev: `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `vitest`, `jsdom`, `@playwright/test`, `axe-core`
4. Configure `tailwind.config.js` — content paths to `./src/**/*.{ts,tsx}`
5. Configure `postcss.config.js` — tailwindcss + autoprefixer
6. Write `src/index.css` — ONLY `@tailwind base/components/utilities`
7. Create `.env.example` / `.env` — `VITE_API_URL=http://localhost:8080`
8. Configure `vite.config.ts` — port 5173, proxy `/api` to VITE_API_URL
9. Scripts: `"test": "vitest run"`, `"lint": "tsc --noEmit"`, `"build:check": "npm run build && npm run lint && npm test"`

**Verification:** `npm run dev` → dev server, `npm run build` → bundle, `npx tsc --noEmit` clean

---

### Step 2: Type System + File Validation

**Files:**
- `src/types/document.ts` — DocType enum (`FAQ | TOS | BRAND | HOWTO`), UploadStatus, IngestionResult, DocumentRecord
- `src/types/api.ts` — ApiError, UploadResponse, BatchUploadResponse, DocumentListResponse
- `src/utils/fileValidation.ts` — ALLOWED_MIME_TYPES, ALLOWED_EXTENSIONS, validateFile(), validateFiles()

**Verification:** `npx tsc --noEmit` clean, unit tests for valid/invalid/edge cases

---

### Step 3: API Client + Error Mapping

**Files:**
- `src/api/apiClient.ts` — axios instance, X-Admin-Key interceptor, console.log stripping in prod
- `src/api/adminApi.ts` — uploadDocument(), uploadBatch(), getDocuments()
- `src/utils/errorMapper.ts` — HTTP status → BI message mapping

**Error mapping table:**
| HTTP | Condition | BI Message |
|------|-----------|------------|
| 401 | - | "Kunci admin tidak valid. Silakan periksa kembali." |
| 403 | - | "Anda tidak memiliki izin untuk mengunggah dokumen." |
| 409 | DUPLICATE_HASH | "File ini sudah pernah diunggah. Tidak ada perubahan yang dilakukan." |
| 413 | - | "Ukuran file terlalu besar. Maksimal 10 MB per file." |
| 415 | - | "Format file tidak didukung. Gunakan PDF, DOCX, atau TXT." |
| 429 | - | "Terlalu banyak permintaan. Silakan tunggu beberapa saat." |
| 500 | - | "Terjadi kesalahan saat mengunggah. Silakan coba lagi atau hubungi tim engineering." |
| Network | - | "Gagal terhubung ke server. Periksa koneksi internet Anda." |

**Verification:** Unit tests for errorMapper, API call construction. `grep -r "console.log" dist/` empty after `npm run build`.

---

### Step 4: i18n Messages Module

**File:** `src/i18n/messages.ts` — single `const MSG` object with namespaced keys (adminKey, upload, documentList, result, errors). All BI strings, typed as `const`.

**Verification:** No raw BI strings in any component (manual review + code review checklist).

---

### Step 5: useSessionKey Hook

**File:** `src/hooks/useSessionKey.ts` — `{ key, setKey, clearKey, hasKey }`. sessionStorage only, never localStorage.

**Verification:** Unit test setKey/clearKey against sessionStorage.

---

### Step 6: AdminKeyPrompt Component

**File:** `src/components/AdminKeyPrompt/AdminKeyPrompt.tsx`

- Props: `{ onAuthenticated: () => void }`
- States: idle → validating → error
- Password input with `aria-label` in BI
- Tailwind: centered card, responsive (`max-w-md w-full mx-4`)
- On submit: `setKey(enteredKey)`, call `onAuthenticated()`

**Tests (5):** renders, stores key, empty error, onAuthenticated called, aria-label in BI

**Verification:** `npx vitest run`, manual: key typed → stored → read back

---

### Step 7: UploadPage Component (Core)

**Sub-components:**

**7a. DropZone.tsx** — HTML5 drag-drop + hidden `<input type="file" multiple>`. States: idle (dashed border), dragOver (solid blue). File validation on drop/select. `accept=".pdf,.docx,.txt"`. aria-label in BI.

**7b. DocTypeSelector.tsx** — 4 radio-button cards (FAQ, TOS, BRAND, HOWTO). Selected card highlighted blue. aria-label on fieldset.

**7c. UploadResultCard.tsx** — Per-file feedback. success (green, chunk count, tokens, duration), skipped (amber warning), error (red). Responsive stacking.

**7d. UploadPage.tsx** (parent):
- State machine: idle → filesSelected → uploading → results
- "Unggah Dokumen" disabled until files.length > 0 && docType !== null
- During upload: indeterminate progress bar
- Results: UploadResultCard per file + summary
- "Unggah Semua" for batch, "Hapus Semua File" to reset
- All text from MSG module, aria-labels in BI

**Tests (10):** renders, enables button on file+type, disabled without type, success result, batch results, duplicate warning, 500 → BI message, format validation, progress indicator, aria-labels

**Verification:** `npx vitest run` passes all 10+, `npx tsc --noEmit` clean

---

### Step 8: DocumentList Component

**File:** `src/components/DocumentList/DocumentList.tsx`

- States: loading (skeleton), empty (EmptyState), error (ErrorMessage), loaded (table)
- Table columns: Nama File, Jenis Dokumen, Tanggal Unggah, Status
- Status badges: "Aktif" (green), "Diproses" (amber), "Gagal" (red)
- Date formatting: `Intl.DateTimeFormat` for WIB
- HTML `<table>` with `aria-label` in BI
- Responsive: horizontal scroll below 1024px, full columns at 1280px+
- Tailwind: `w-full border-collapse divide-y even:bg-gray-50`

**Tests (7):** loading state, empty state, table with data, API error, date format, status badge colors, aria-label

**Verification:** `npx vitest run`, `npx tsc --noEmit`

---

### Step 9: Shared Components

- `ProgressIndicator.tsx` — indeterminate animation, `aria-label`
- `ErrorMessage.tsx` — styled banner, optional "Coba Lagi" button
- `EmptyState.tsx` — message + optional icon

**Verification:** Render tests, axe-core check on each.

---

### Step 10: App Shell + Routing

**File:** `src/App.tsx`

Layout:
```
+--------------------------------------------+
| Header: "Dashboard Admin KP" | [Keluar]     |
+--------------------------------------------+
| Tab: [Unggah Dokumen] [Daftar Dokumen]     |
+--------------------------------------------+
| <UploadPage /> or <DocumentList />          |
+--------------------------------------------+
```

- `useSessionKey()` gate: no key → AdminKeyPrompt, has key → dashboard
- "Keluar" button → clearKey(), back to prompt
- Tab navigation: state-based or react-router-dom with 2 routes
- Responsive: min-w-[320px], full layout at 1280px+

**Verification:** Full manual walkthrough: key → dashboard → upload → list → logout → prompt. 1280x720 viewport works. No console errors.

---

### Step 11: E2E Tests (Playwright)

**File:** `e2e/admin-upload.spec.ts`

Scenarios:
1. Login flow: prompt visible → enter key → dashboard renders
2. Single upload: drop PDF → select FAQ → click → result card with chunk count
3. Duplicate warning: upload same file twice → skip message
4. Batch with mixed results: 3 files (2 new, 1 dup) → 2 success + 1 skipped
5. Error handling: mock 500 → friendly BI message, no stack trace
6. Document list: navigate to tab → table with uploaded docs
7. Logout: click Keluar → prompt shown, sessionStorage empty

**Config:** Chromium + Firefox + WebKit, screenshots on failure, webServer on port 5173.

**Verification:** `npx playwright test` passes all scenarios.

---

### Step 12: Accessibility Audit

- `jest-axe` integration in all RTL tests: `expect(violations).toHaveLength(0)`
- `@axe-core/playwright` E2E test: `e2e/a11y.spec.ts`
- Checklist: aria-labels in BI, color contrast WCAG AA, focus indicators, logical tab order

**Verification:** 0 axe violations across all pages, all components.

---

### Step 13: Production Build Verification

1. `npm run build` success
2. `grep -r "console.log" dist/` → zero matches
3. `grep -r "localhost" dist/` → zero matches
4. `grep -r "REACT_APP_" dist/` → zero matches
5. Bundle < 200 KB gzipped
6. `npm run build:check` pass (build + lint + test)

---

## 4. Dependency Graph

```
Step 1: Scaffold
  └── Step 2: Types + File Validation
        ├── Step 3: API Client + Error Mapper
        └── Step 4: i18n Messages
              └── Step 5: useSessionKey Hook
                    └── Step 6: AdminKeyPrompt
                          ├── Step 7: UploadPage
                          └── Step 8: DocumentList
                                ├── Step 9: Shared Components
                                └── Step 10: App Shell
                                      ├── Step 11: E2E Tests
                                      ├── Step 12: Accessibility Audit
                                      └── Step 13: Production Build
```

Steps 7 and 8 are independent after Step 6.

---

## 5. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| `GET /admin/documents` endpoint missing | High | Cache upload responses; escalate to backend agent |
| Admin key per-page (not per-session) | Medium | Refactor AdminKeyPrompt to route-level gate |
| Figma/design mockup missing | Medium | PRD is detailed enough; follow standard admin patterns |
| MiniMax exposed to browser | Critical | Code review must verify zero `api.minimax.chat` references |
| Tailwind purging removes dynamic classes | Medium | Full class names, no string concatenation; content paths correct |
| Bundle size exceeds budget | Low | Vite + React 18 + Tailwind lightweight; Intl.DateTimeFormat for dates |

---

## 6. Branch & Commit Strategy

- **Branch:** `feat/frontend-admin-document-upload-ui` (from `main`)
- **Commits:** Conventional Commits (`feat(frontend):`, `test(frontend):`)
- **PR:** Draft initially → Ready after: all tests pass, axe 0 violations, Playwright passes 3 browsers, screenshots attached

---

## 7. File Checklist

| # | File | Step |
|---|------|------|
| 1 | `frontend/package.json` | 1 |
| 2 | `frontend/tsconfig.json` | 1 |
| 3 | `frontend/vite.config.ts` | 1 |
| 4 | `frontend/tailwind.config.js` | 1 |
| 5 | `frontend/postcss.config.js` | 1 |
| 6 | `frontend/index.html` | 1 |
| 7 | `frontend/.env.example` / `.env` | 1 |
| 8 | `frontend/src/main.tsx` | 1 |
| 9 | `frontend/src/index.css` | 1 |
| 10 | `frontend/src/types/document.ts` | 2 |
| 11 | `frontend/src/types/api.ts` | 2 |
| 12 | `frontend/src/utils/fileValidation.ts` | 2 |
| 13 | `frontend/src/utils/errorMapper.ts` | 3 |
| 14 | `frontend/src/api/apiClient.ts` | 3 |
| 15 | `frontend/src/api/adminApi.ts` | 3 |
| 16 | `frontend/src/i18n/messages.ts` | 4 |
| 17 | `frontend/src/hooks/useSessionKey.ts` | 5 |
| 18 | `frontend/src/components/AdminKeyPrompt/AdminKeyPrompt.tsx` | 6 |
| 19 | `frontend/src/components/AdminKeyPrompt/AdminKeyPrompt.test.tsx` | 6 |
| 20 | `frontend/src/components/UploadPage/DropZone.tsx` | 7 |
| 21 | `frontend/src/components/UploadPage/DocTypeSelector.tsx` | 7 |
| 22 | `frontend/src/components/UploadPage/UploadResultCard.tsx` | 7 |
| 23 | `frontend/src/components/UploadPage/UploadPage.tsx` | 7 |
| 24 | `frontend/src/components/UploadPage/UploadPage.test.tsx` | 7 |
| 25 | `frontend/src/components/DocumentList/DocumentList.tsx` | 8 |
| 26 | `frontend/src/components/DocumentList/DocumentList.test.tsx` | 8 |
| 27 | `frontend/src/components/shared/ProgressIndicator.tsx` | 9 |
| 28 | `frontend/src/components/shared/ErrorMessage.tsx` | 9 |
| 29 | `frontend/src/components/shared/EmptyState.tsx` | 9 |
| 30 | `frontend/src/App.tsx` | 10 |
| 31 | `frontend/playwright.config.ts` | 11 |
| 32 | `frontend/e2e/admin-upload.spec.ts` | 11 |
| 33 | `frontend/e2e/a11y.spec.ts` | 12 |
| 34 | `frontend/src/test-utils/a11y.ts` | 12 |

---

## 8. PRD Acceptance Criteria Traceability

| PRD Scenario | Covered By |
|--------------|-----------|
| Single file upload + ingestion feedback | Step 7 UploadPage, E2E test |
| Duplicate file skipped with clear warning | Step 7 UploadResultCard, E2E test |
| Batch upload with mixed results | Step 7 UploadPage (batch), E2E test |
| API error mapped to friendly message | Step 3 errorMapper, Step 7, E2E test |

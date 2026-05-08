# ADR-004: RAG Chunking and Retrieval Improvements

## Status
Accepted

## Date
2026-05-08

## Context

Users reported "missing context" from pgvector during chat — answers were incomplete or lacked surrounding detail. Investigation found six root causes in the chunking-embedding-retrieval pipeline:

### Root Causes

**1. Chunk size too small (400 chars / ~100 tokens)**
Concepts spanning multiple sentences get split. The retrieved chunk lacks surrounding context, so the LLM gets an incomplete fragment.

**2. No adjacent chunk retrieval**
When chunk N is relevant, chunks N-1 and N+1 (which complete the thought) are never fetched. The LLM sees a fragment without setup or conclusion.

**3. docMaxChars (1200) smaller than top-K capacity (5 × 400 = 2000 chars)**
2 of every 5 retrieved chunks are silently dropped before reaching the LLM, discarding relevant content.

**4. No conversation context in search query**
Follow-up questions ("what about the fee?", "tell me more") lack subject/pronoun resolution. The search returns unrelated chunks because only the bare follow-up is embedded.

**5. Similarity threshold too permissive (0.3)**
Low-relevance chunks fill the top-K slots, crowding out better matches.

**6. No whitespace normalization before chunking**
PDF/OCR artifacts (multiple spaces, excessive newlines) waste chunk character budget and degrade embedding quality.

## Decision

Apply all six fixes in priority order:

| Priority | Fix | Change |
|----------|-----|--------|
| 1 | docMaxChars | 1200 → 3000 |
| 2 | Chunk size | 400 → 800 chars |
| 3 | Overlap | 50 → 100 chars |
| 4 | Whitespace normalization | Add to TextChunker before split |
| 5 | Adjacent chunk expansion | Fetch chunk N-1, N+1 when similarity ≥ 0.6 |
| 6 | Conversation-aware query | Prepend last 2 messages to search query |
| 7 | Similarity threshold | 0.3 → 0.5 |

No new dependencies required. All changes are in existing files plus one new Flyway migration.

## Consequences

**Positive:**
- More complete context per retrieved chunk (fewer mid-sentence cuts)
- Adjacent chunks fill gaps between retrieved fragments
- No silent chunk drops (docMaxChars now exceeds realistic retrieval size)
- Follow-up questions find correct context via conversation-augmented query
- Less noise from low-similarity matches

**Negative / Trade-offs:**
- Larger chunks = fewer total chunks per document = slightly lower recall diversity (mitigated by adjacent expansion)
- Re-ingestion of all documents required after chunk-size change
- Adjacent expansion adds 1 extra DB query per high-confidence result (acceptable — queries are cheap)
- Conversation-aware query increases embedding token usage by ~2× (negligible cost)

## Alternatives Considered

**Cross-encoder re-ranking:** Would improve relevance ordering but requires an additional model call per result. Deferred — current fixes address the root causes at lower cost.

**Hybrid BM25 + vector search:** Improves keyword-heavy queries. Deferred — requires `pg_bm25` extension or external index. May revisit if keyword recall remains poor after these fixes.

**HNSW index instead of IVFFlat:** HNSW gives better recall at any dataset size. Since re-ingestion is required anyway (chunk-size change), a V7 migration is included as optional.

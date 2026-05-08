-- Replace IVFFlat with HNSW for better recall at any dataset size.
-- IVFFlat requires ~39x lists rows for quality; HNSW works well regardless of row count.
DROP INDEX IF EXISTS idx_chunks_embedding_cosine;

CREATE INDEX idx_chunks_embedding_hnsw ON chunks
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

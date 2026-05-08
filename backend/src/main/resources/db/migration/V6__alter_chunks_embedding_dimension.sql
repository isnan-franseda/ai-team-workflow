-- Resize embedding column to match minimax-m2.7 output (1024 dims, not 1536)
DROP INDEX IF EXISTS idx_chunks_embedding_cosine;

ALTER TABLE chunks
    ALTER COLUMN embedding TYPE vector(1024);

CREATE INDEX idx_chunks_embedding_cosine ON chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

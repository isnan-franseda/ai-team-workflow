-- Chunks table: stores document chunks with vector embeddings
CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doc_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    chunk_index INT NOT NULL,
    token_count INT NOT NULL DEFAULT 0,
    embedding vector(1536),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Index for looking up chunks by document
CREATE INDEX idx_chunks_doc_id ON chunks(doc_id);

-- IVFFlat index for cosine similarity search on embeddings
CREATE INDEX idx_chunks_embedding_cosine ON chunks
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
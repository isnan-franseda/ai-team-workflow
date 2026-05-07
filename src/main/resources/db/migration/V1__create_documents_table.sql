-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Documents table: stores metadata about ingested documents
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(500) NOT NULL,
    doc_type VARCHAR(50) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Unique constraint on file_hash for idempotency
CREATE UNIQUE INDEX idx_documents_file_hash ON documents(file_hash);
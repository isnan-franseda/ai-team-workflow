CREATE TABLE ingestion_logs (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID        NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    filename    VARCHAR(500) NOT NULL,
    doc_type    VARCHAR(50)  NOT NULL,
    status      VARCHAR(10)  NOT NULL CHECK (status IN ('SUCCESS', 'SKIPPED', 'FAILED')),
    chunks_created  INT     NOT NULL DEFAULT 0,
    tokens_used     INT     NOT NULL DEFAULT 0,
    error_message   TEXT,
    duration_ms     BIGINT  NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_logs_document_id ON ingestion_logs(document_id);
CREATE INDEX idx_ingestion_logs_status ON ingestion_logs(status);
CREATE INDEX idx_ingestion_logs_created_at ON ingestion_logs(created_at DESC);

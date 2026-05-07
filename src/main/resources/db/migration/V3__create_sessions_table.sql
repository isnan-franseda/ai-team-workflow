-- Sessions table: conversation sessions
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    last_active_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
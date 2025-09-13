-- Align LaTeX AI Chat schema with current JPA entities
-- This migration assumes V7 previously created UUID-based sessions/messages tables but with mismatches for checkpoints.
-- We will recreate the affected tables with correct columns and constraints.

-- Drop existing tables to avoid type mismatches (data loss acceptable for development/testing)
DROP TABLE IF EXISTS latex_document_checkpoints CASCADE;
DROP TABLE IF EXISTS latex_ai_chat_messages CASCADE;
DROP TABLE IF EXISTS latex_ai_chat_sessions CASCADE;

-- Ensure pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Sessions: UUID PK; document_id UUID; project_id UUID; titles and flags
CREATE TABLE latex_ai_chat_sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID NOT NULL,
    project_id UUID NOT NULL,
    session_title VARCHAR(255) NOT NULL DEFAULT 'LaTeX AI Chat',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

-- Messages: BIGSERIAL PK; session_id UUID FK; matching entity columns
CREATE TABLE latex_ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    message_type VARCHAR(10) NOT NULL CHECK (message_type IN ('USER','AI')),
    content TEXT NOT NULL,
    latex_suggestion TEXT,
    action_type VARCHAR(20) CHECK (action_type IN ('ADD','REPLACE','DELETE','MODIFY')),
    selection_range_from INTEGER,
    selection_range_to INTEGER,
    cursor_position INTEGER,
    is_applied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_latex_chat_message_session FOREIGN KEY (session_id)
        REFERENCES latex_ai_chat_sessions(id) ON DELETE CASCADE
);

-- Checkpoints: BIGSERIAL PK; document_id UUID; session_id UUID FK; message_id BIGINT FK (nullable);
-- with proper columns and flags
CREATE TABLE latex_document_checkpoints (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL,
    session_id UUID NOT NULL,
    message_id BIGINT,
    checkpoint_name VARCHAR(255) NOT NULL,
    content_before TEXT NOT NULL,
    content_after TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_current BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_latex_checkpoint_session FOREIGN KEY (session_id)
        REFERENCES latex_ai_chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_latex_checkpoint_message FOREIGN KEY (message_id)
        REFERENCES latex_ai_chat_messages(id) ON DELETE SET NULL
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_latex_chat_sessions_document_id ON latex_ai_chat_sessions(document_id);
CREATE INDEX IF NOT EXISTS idx_latex_chat_sessions_project_id ON latex_ai_chat_sessions(project_id);
CREATE INDEX IF NOT EXISTS idx_latex_chat_messages_session_id ON latex_ai_chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_latex_chat_messages_created_at ON latex_ai_chat_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_document_id ON latex_document_checkpoints(document_id);
CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_session_id ON latex_document_checkpoints(session_id);
CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_current ON latex_document_checkpoints(document_id, is_current);

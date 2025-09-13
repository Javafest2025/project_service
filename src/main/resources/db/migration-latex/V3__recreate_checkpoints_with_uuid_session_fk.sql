-- Recreate checkpoints table with UUID session_id and proper FKs
-- This fixes: ERROR: column "session_id" is of type bigint but expression is of type uuid

BEGIN;

-- Preserve data if needed by copying to a temp table (skipped here; dev environment)
DROP TABLE IF EXISTS latex_document_checkpoints CASCADE;

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

CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_document_id ON latex_document_checkpoints(document_id);
CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_session_id ON latex_document_checkpoints(session_id);
CREATE INDEX IF NOT EXISTS idx_latex_checkpoints_current ON latex_document_checkpoints(document_id, is_current);

COMMIT;

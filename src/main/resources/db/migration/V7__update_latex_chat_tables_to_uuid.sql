-- Update LaTeX AI Chat Tables to use UUID for all ID fields
-- V7__update_latex_chat_tables_to_uuid.sql

-- First, drop all foreign key constraints
ALTER TABLE latex_ai_chat_messages DROP CONSTRAINT IF EXISTS fk_latex_chat_message_session;
ALTER TABLE latex_ai_chat_sessions DROP CONSTRAINT IF EXISTS fk_latex_chat_session_document;
ALTER TABLE latex_ai_chat_sessions DROP CONSTRAINT IF EXISTS fk_latex_chat_session_project;
ALTER TABLE latex_document_checkpoints DROP CONSTRAINT IF EXISTS fk_latex_checkpoint_document;

-- Drop and recreate tables to handle UUID conversion properly
-- Note: This will clear existing data - backup if needed

DROP TABLE IF EXISTS latex_ai_chat_messages CASCADE;
DROP TABLE IF EXISTS latex_document_checkpoints CASCADE;
DROP TABLE IF EXISTS latex_ai_chat_sessions CASCADE;

-- Recreate latex_ai_chat_sessions table with UUID
CREATE TABLE latex_ai_chat_sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    document_id UUID NOT NULL,
    project_id UUID NOT NULL,
    session_name VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Recreate latex_ai_chat_messages table with UUID session_id
CREATE TABLE latex_ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id UUID NOT NULL,
    message TEXT NOT NULL,
    sender VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_latex_chat_message_session 
        FOREIGN KEY (session_id) REFERENCES latex_ai_chat_sessions(id) ON DELETE CASCADE
);

-- Recreate latex_document_checkpoints table with UUID
CREATE TABLE latex_document_checkpoints (
    id BIGSERIAL PRIMARY KEY,
    document_id UUID NOT NULL,
    checkpoint_text TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
); 
--     FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;

-- ALTER TABLE latex_ai_chat_sessions 
--     ADD CONSTRAINT fk_latex_chat_session_project 
--     FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE;

-- ALTER TABLE latex_document_checkpoints 
--     ADD CONSTRAINT fk_latex_checkpoint_document 
--     FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE;
-- Create LaTeX AI Chat System Tables
-- V6__create_latex_ai_chat_tables.sql

-- Table to store LaTeX AI chat sessions (one per document)
CREATE TABLE IF NOT EXISTS latex_ai_chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    session_title VARCHAR(255) NOT NULL DEFAULT 'LaTeX AI Chat',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    
    CONSTRAINT fk_latex_chat_session_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_latex_chat_session_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    
    -- Ensure one chat session per document
    UNIQUE(document_id)
);

-- Table to store individual chat messages
CREATE TABLE IF NOT EXISTS latex_ai_chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    message_type VARCHAR(10) NOT NULL CHECK (message_type IN ('USER', 'AI')),
    content TEXT NOT NULL,
    latex_suggestion TEXT,
    action_type VARCHAR(20) CHECK (action_type IN ('ADD', 'REPLACE', 'DELETE', 'MODIFY')),
    selection_range_from INTEGER,
    selection_range_to INTEGER,
    cursor_position INTEGER,
    is_applied BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_latex_chat_message_session FOREIGN KEY (session_id) REFERENCES latex_ai_chat_sessions(id) ON DELETE CASCADE
);

-- Table to store document checkpoints (for restore functionality)
CREATE TABLE IF NOT EXISTS latex_document_checkpoints (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    message_id BIGINT, -- The message that triggered this checkpoint
    checkpoint_name VARCHAR(255) NOT NULL,
    content_before TEXT NOT NULL, -- Document content before AI modification
    content_after TEXT, -- Document content after AI modification (nullable if not applied yet)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_current BOOLEAN DEFAULT FALSE, -- Mark the current active checkpoint
    
    CONSTRAINT fk_latex_checkpoint_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_latex_checkpoint_session FOREIGN KEY (session_id) REFERENCES latex_ai_chat_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_latex_checkpoint_message FOREIGN KEY (message_id) REFERENCES latex_ai_chat_messages(id) ON DELETE SET NULL
);

-- Indexes for better performance (conditional creation)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_chat_sessions_document_id') THEN
        CREATE INDEX idx_latex_chat_sessions_document_id ON latex_ai_chat_sessions(document_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_chat_sessions_project_id') THEN
        CREATE INDEX idx_latex_chat_sessions_project_id ON latex_ai_chat_sessions(project_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_chat_messages_session_id') THEN
        CREATE INDEX idx_latex_chat_messages_session_id ON latex_ai_chat_messages(session_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_chat_messages_created_at') THEN
        CREATE INDEX idx_latex_chat_messages_created_at ON latex_ai_chat_messages(created_at);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_checkpoints_document_id') THEN
        CREATE INDEX idx_latex_checkpoints_document_id ON latex_document_checkpoints(document_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_checkpoints_session_id') THEN
        CREATE INDEX idx_latex_checkpoints_session_id ON latex_document_checkpoints(session_id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_latex_checkpoints_current') THEN
        CREATE INDEX idx_latex_checkpoints_current ON latex_document_checkpoints(document_id, is_current);
    END IF;
END $$;

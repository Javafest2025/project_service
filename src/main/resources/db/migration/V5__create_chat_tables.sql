-- Add chat-related columns to papers table and create chat tables
-- V5: Add chat session and message tables for PDF contextual Q&A

-- Add extraction-related columns to papers table if they don't exist
ALTER TABLE papers 
ADD COLUMN IF NOT EXISTS extraction_job_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS extraction_started_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS extraction_completed_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS extraction_error TEXT,
ADD COLUMN IF NOT EXISTS extraction_status VARCHAR(50) DEFAULT 'NOT_STARTED',
ADD COLUMN IF NOT EXISTS extraction_coverage DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS sections_count INTEGER,
ADD COLUMN IF NOT EXISTS figures_count INTEGER,
ADD COLUMN IF NOT EXISTS tables_count INTEGER,
ADD COLUMN IF NOT EXISTS equations_count INTEGER,
ADD COLUMN IF NOT EXISTS references_count INTEGER,
ADD COLUMN IF NOT EXISTS page_count INTEGER;

-- Create chat_sessions table
CREATE TABLE IF NOT EXISTS chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paper_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at TIMESTAMP,
    message_count INTEGER DEFAULT 0,
    CONSTRAINT fk_chat_sessions_paper_id FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- Create chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    token_count INTEGER,
    context_sections TEXT[], -- Array of section IDs that were used for context
    confidence_score DOUBLE PRECISION,
    
    CONSTRAINT fk_chat_messages_session_id FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_chat_sessions_paper_id ON chat_sessions(paper_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_chat_sessions_updated_at ON chat_sessions(updated_at);

CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_timestamp ON chat_messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_chat_messages_role ON chat_messages(role);

-- Create index for paper extraction status queries
CREATE INDEX IF NOT EXISTS idx_papers_extraction_status ON papers(extraction_status);
CREATE INDEX IF NOT EXISTS idx_papers_is_extracted ON papers(is_extracted);

-- Add trigger to update updated_at timestamp in chat_sessions
CREATE OR REPLACE FUNCTION update_chat_session_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_chat_session_timestamp
    BEFORE UPDATE ON chat_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_session_timestamp();

-- Add trigger to update session's last_message_at and message_count when messages are added
CREATE OR REPLACE FUNCTION update_session_on_message_insert()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chat_sessions 
    SET 
        last_message_at = NEW.timestamp,
        message_count = message_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.session_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_session_on_message_insert
    AFTER INSERT ON chat_messages
    FOR EACH ROW
    EXECUTE FUNCTION update_session_on_message_insert();

-- Add content_hash column to citation_checks table for reuse optimization
-- V14__add_content_hash_to_citation_checks.sql

-- Add content_hash column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'citation_checks' 
        AND column_name = 'content_hash'
    ) THEN
        ALTER TABLE citation_checks 
        ADD content_hash VARCHAR(64);
    END IF;
END $$;

-- Create index for content hash queries (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_citation_checks_content_hash'
    ) THEN
        CREATE INDEX idx_citation_checks_content_hash ON citation_checks(content_hash);
    END IF;
END $$;

-- Add combined index for fast lookup by document + hash + status
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_citation_checks_doc_hash_status'
    ) THEN
        CREATE INDEX idx_citation_checks_doc_hash_status ON citation_checks(document_id, content_hash, status);
    END IF;
END $$;

COMMENT ON COLUMN citation_checks.content_hash IS 'SHA256 hash of LaTeX content for reuse detection and cache invalidation';
-- Add resolved column to citation_issues table
-- V10__add_resolved_to_citation_issues.sql

-- Add resolved column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'citation_issues' 
        AND column_name = 'resolved'
    ) THEN
        ALTER TABLE citation_issues 
        ADD resolved BOOLEAN NOT NULL DEFAULT false;
    END IF;
END $$;

-- Create index for resolved status queries (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_citation_issues_resolved'
    ) THEN
        CREATE INDEX idx_citation_issues_resolved ON citation_issues(resolved);
    END IF;
END $$;
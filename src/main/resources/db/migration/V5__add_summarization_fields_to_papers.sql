-- Add summarization-related fields to papers table
-- Add is_summarized column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'is_summarized'
    ) THEN
        ALTER TABLE papers ADD COLUMN is_summarized BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Add summarization_status column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'summarization_status'
    ) THEN
        ALTER TABLE papers ADD COLUMN summarization_status VARCHAR(50);
    END IF;
END $$;

-- Add summarization_started_at column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'summarization_started_at'
    ) THEN
        ALTER TABLE papers ADD COLUMN summarization_started_at TIMESTAMP;
    END IF;
END $$;

-- Add summarization_completed_at column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'summarization_completed_at'
    ) THEN
        ALTER TABLE papers ADD COLUMN summarization_completed_at TIMESTAMP;
    END IF;
END $$;

-- Add summarization_error column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'summarization_error'
    ) THEN
        ALTER TABLE papers ADD COLUMN summarization_error TEXT;
    END IF;
END $$;

-- Add index on summarization status for better query performance (only if they don't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_papers_summarization_status'
    ) THEN
        CREATE INDEX idx_papers_summarization_status ON papers(summarization_status);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_papers_is_summarized'
    ) THEN
        CREATE INDEX idx_papers_is_summarized ON papers(is_summarized);
    END IF;
END $$;

-- Add response source tracking to paper_summaries table
-- Add response_source column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'paper_summaries' 
        AND column_name = 'response_source'
    ) THEN
        ALTER TABLE paper_summaries ADD COLUMN response_source VARCHAR(50);
    END IF;
END $$;

-- Add fallback_reason column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'paper_summaries' 
        AND column_name = 'fallback_reason'
    ) THEN
        ALTER TABLE paper_summaries ADD COLUMN fallback_reason TEXT;
    END IF;
END $$;

-- Add comment to explain the new fields
COMMENT ON COLUMN paper_summaries.response_source IS 'Source of the summary: GEMINI_API, FALLBACK, CACHED, or MANUAL';
COMMENT ON COLUMN paper_summaries.fallback_reason IS 'Reason for fallback response when Gemini API is unavailable';

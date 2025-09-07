-- Add response source tracking to paper_summaries table
ALTER TABLE paper_summaries 
ADD COLUMN response_source VARCHAR(50),
ADD COLUMN fallback_reason TEXT;

-- Add comment to explain the new fields
COMMENT ON COLUMN paper_summaries.response_source IS 'Source of the summary: GEMINI_API, FALLBACK, CACHED, or MANUAL';
COMMENT ON COLUMN paper_summaries.fallback_reason IS 'Reason for fallback response when Gemini API is unavailable';

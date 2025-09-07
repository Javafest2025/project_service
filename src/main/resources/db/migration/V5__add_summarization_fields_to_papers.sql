-- Add summarization-related fields to papers table
ALTER TABLE papers 
ADD COLUMN is_summarized BOOLEAN DEFAULT FALSE,
ADD COLUMN summarization_status VARCHAR(50),
ADD COLUMN summarization_started_at TIMESTAMP,
ADD COLUMN summarization_completed_at TIMESTAMP,
ADD COLUMN summarization_error TEXT;

-- Add index on summarization status for better query performance
CREATE INDEX idx_papers_summarization_status ON papers(summarization_status);
CREATE INDEX idx_papers_is_summarized ON papers(is_summarized);

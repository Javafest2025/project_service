-- V4__add_latex_context_column_to_papers.sql
-- Migration description: Add is_latex_context column to papers table for LaTeX editor context management

-- Add the new column with default value false (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'papers' 
        AND column_name = 'is_latex_context'
    ) THEN
        ALTER TABLE papers ADD COLUMN is_latex_context BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Add index for better query performance when filtering by LaTeX context (only if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes 
        WHERE indexname = 'idx_papers_latex_context'
    ) THEN
        CREATE INDEX idx_papers_latex_context ON papers(is_latex_context);
    END IF;
END $$;

-- Add comment to document the column purpose
COMMENT ON COLUMN papers.is_latex_context IS 'Indicates if the paper is added to LaTeX context for the project';

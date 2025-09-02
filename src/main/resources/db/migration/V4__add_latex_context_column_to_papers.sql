-- V4__add_latex_context_column_to_papers.sql
-- Migration description: Add is_latex_context column to papers table for LaTeX editor context management

-- Add the new column with default value false
ALTER TABLE papers ADD COLUMN is_latex_context BOOLEAN DEFAULT FALSE;

-- Add index for better query performance when filtering by LaTeX context
CREATE INDEX idx_papers_latex_context ON papers(is_latex_context);

-- Add comment to document the column purpose
COMMENT ON COLUMN papers.is_latex_context IS 'Indicates if the paper is added to LaTeX context for the project';

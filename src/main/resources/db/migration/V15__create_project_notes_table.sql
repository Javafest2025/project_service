-- Create project_notes table for storing project notes
CREATE TABLE IF NOT EXISTS project_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    tags TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Add foreign key constraint to projects table
    CONSTRAINT fk_project_notes_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE
);

-- Create index on project_id for faster queries
CREATE INDEX IF NOT EXISTS idx_project_notes_project_id ON project_notes(project_id);

-- Create index on is_favorite for filtering favorite notes
CREATE INDEX IF NOT EXISTS idx_project_notes_is_favorite ON project_notes(is_favorite);

-- Create index on updated_at for ordering notes
CREATE INDEX IF NOT EXISTS idx_project_notes_updated_at ON project_notes(updated_at DESC);

-- Add comment to the table
COMMENT ON TABLE project_notes IS 'Stores notes associated with research projects';
COMMENT ON COLUMN project_notes.project_id IS 'Foreign key to projects table';
COMMENT ON COLUMN project_notes.is_favorite IS 'Flag to mark notes as favorites';
COMMENT ON COLUMN project_notes.tags IS 'Array of tags for categorizing notes';

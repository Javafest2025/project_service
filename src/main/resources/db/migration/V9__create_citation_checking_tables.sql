-- Citation Checking System Database Migration
-- V9__create_citation_checking_tables.sql

-- Create citation checks table (stores one job per run, overwrites latest per document when DONE)
CREATE TABLE IF NOT EXISTS citation_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    document_id UUID NOT NULL,
    tex_file_name TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'ERROR')),
    step TEXT NOT NULL CHECK (step IN ('PARSING', 'LOCAL_RETRIEVAL', 'LOCAL_VERIFICATION', 'WEB_RETRIEVAL', 'WEB_VERIFICATION', 'SAVING', 'DONE', 'ERROR')),
    progress_pct INTEGER NOT NULL DEFAULT 0 CHECK (progress_pct >= 0 AND progress_pct <= 100),
    summary JSONB DEFAULT '{}',  -- { total: number, byType: Record<CitationIssueType, number> }
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_citation_check_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_check_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create citation issues table (stores issues for a job)
CREATE TABLE IF NOT EXISTS citation_issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    project_id UUID NOT NULL,
    document_id UUID NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('missing-citation', 'weak-citation', 'orphan-reference', 'incorrect-metadata', 'plausible-claim-no-source', 'possible-plagiarism')),
    severity TEXT NOT NULL CHECK (severity IN ('low', 'medium', 'high')),
    from_pos INTEGER NOT NULL CHECK (from_pos >= 0),
    to_pos INTEGER NOT NULL CHECK (to_pos >= from_pos),
    line_start INTEGER NOT NULL CHECK (line_start >= 1),
    line_end INTEGER NOT NULL CHECK (line_end >= line_start),
    snippet TEXT NOT NULL,
    cited_keys TEXT[] NOT NULL DEFAULT '{}',
    suggestions JSONB NOT NULL DEFAULT '[]',  -- Array of suggestion objects
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_citation_issue_job FOREIGN KEY (job_id) REFERENCES citation_checks(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_issue_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_citation_issue_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create citation evidence table (stores evidence per issue)
CREATE TABLE IF NOT EXISTS citation_evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    issue_id UUID NOT NULL,
    source JSONB NOT NULL,  -- { kind: 'local'|'web', paperId?, url?, paperTitle?, sectionId?, paragraphId?, page?, domain? }
    matched_text TEXT NOT NULL,
    similarity DOUBLE PRECISION CHECK (similarity >= 0 AND similarity <= 1),
    support_score DOUBLE PRECISION CHECK (support_score >= 0 AND support_score <= 1),
    extra JSONB DEFAULT '{}',  -- Additional metadata
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_citation_evidence_issue FOREIGN KEY (issue_id) REFERENCES citation_issues(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_citation_checks_project_id ON citation_checks(project_id);
CREATE INDEX idx_citation_checks_document_id ON citation_checks(document_id);
CREATE INDEX idx_citation_checks_status ON citation_checks(status);
CREATE INDEX idx_citation_checks_created_at ON citation_checks(created_at);

CREATE INDEX idx_citation_issues_job_id ON citation_issues(job_id);
CREATE INDEX idx_citation_issues_project_id ON citation_issues(project_id);
CREATE INDEX idx_citation_issues_document_id ON citation_issues(document_id);
CREATE INDEX idx_citation_issues_type ON citation_issues(type);
CREATE INDEX idx_citation_issues_severity ON citation_issues(severity);

CREATE INDEX idx_citation_evidence_issue_id ON citation_evidence(issue_id);

-- Create partial unique index for "latest per document" - ensures only one DONE status per document
CREATE UNIQUE INDEX idx_citation_latest_per_document 
ON citation_checks(document_id) 
WHERE status = 'DONE';

-- Update trigger for citation_checks updated_at
CREATE OR REPLACE FUNCTION update_citation_checks_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_citation_checks_updated_at
    BEFORE UPDATE ON citation_checks
    FOR EACH ROW
    EXECUTE FUNCTION update_citation_checks_updated_at();

-- Add comments for documentation
COMMENT ON TABLE citation_checks IS 'Stores citation checking jobs, one per document run with status tracking';
COMMENT ON TABLE citation_issues IS 'Individual citation issues found during checking with location and suggestion data';
COMMENT ON TABLE citation_evidence IS 'Evidence supporting or contradicting citation issues, from local papers or web sources';

COMMENT ON COLUMN citation_checks.summary IS 'JSON summary of issue counts by type: { total: number, byType: Record<CitationIssueType, number> }';
COMMENT ON COLUMN citation_checks.step IS 'Current processing step for tracking progress';
COMMENT ON COLUMN citation_checks.progress_pct IS 'Progress percentage from 0 to 100';

COMMENT ON COLUMN citation_issues.from_pos IS 'Start character position in LaTeX document (0-based)';
COMMENT ON COLUMN citation_issues.to_pos IS 'End character position in LaTeX document (0-based)';
COMMENT ON COLUMN citation_issues.line_start IS 'Start line number in LaTeX document (1-based)';
COMMENT ON COLUMN citation_issues.line_end IS 'End line number in LaTeX document (1-based)';
COMMENT ON COLUMN citation_issues.cited_keys IS 'Array of bibliography keys cited in the problematic text span';
COMMENT ON COLUMN citation_issues.suggestions IS 'JSON array of suggested citations with metadata';

COMMENT ON COLUMN citation_evidence.source IS 'JSON object describing evidence source (local paper or web URL)';
COMMENT ON COLUMN citation_evidence.matched_text IS 'Text snippet that provides evidence for or against the citation issue';
COMMENT ON COLUMN citation_evidence.similarity IS 'Semantic similarity score between issue text and evidence (0-1)';
COMMENT ON COLUMN citation_evidence.support_score IS 'NLI-style confidence that evidence supports the claim (0-1)';
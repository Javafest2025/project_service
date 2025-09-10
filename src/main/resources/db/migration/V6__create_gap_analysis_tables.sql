-- Create gap analysis tables
-- V6: Add gap analysis functionality with one-to-many relationship between papers and gap analyses

-- Create gap_analyses table
CREATE TABLE IF NOT EXISTS gap_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    paper_id UUID NOT NULL,
    paper_extraction_id UUID NOT NULL,
    correlation_id VARCHAR(100) UNIQUE NOT NULL,
    request_id VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    config TEXT,
    total_gaps_identified INTEGER DEFAULT 0,
    valid_gaps_count INTEGER DEFAULT 0,
    invalid_gaps_count INTEGER DEFAULT 0,
    modified_gaps_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gap_analyses_paper_id FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE
);

-- Create research_gaps table
CREATE TABLE IF NOT EXISTS research_gaps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gap_analysis_id UUID NOT NULL,
    gap_id VARCHAR(100) UNIQUE NOT NULL,
    order_index INTEGER,
    name VARCHAR(500),
    description TEXT,
    category VARCHAR(100),
    validation_status VARCHAR(50) DEFAULT 'INITIAL',
    validation_confidence DOUBLE PRECISION,
    initial_reasoning TEXT,
    initial_evidence TEXT,
    validation_query TEXT,
    papers_analyzed_count INTEGER DEFAULT 0,
    validation_reasoning TEXT,
    modification_history TEXT,
    potential_impact TEXT,
    research_hints TEXT,
    implementation_suggestions TEXT,
    risks_and_challenges TEXT,
    required_resources TEXT,
    estimated_difficulty VARCHAR(50),
    estimated_timeline VARCHAR(100),
    evidence_anchors TEXT,
    supporting_papers TEXT,
    conflicting_papers TEXT,
    suggested_topics TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    validated_at TIMESTAMP,
    CONSTRAINT fk_research_gaps_gap_analysis_id FOREIGN KEY (gap_analysis_id) REFERENCES gap_analyses(id) ON DELETE CASCADE
);

-- Create gap_validation_papers table
CREATE TABLE IF NOT EXISTS gap_validation_papers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    research_gap_id UUID NOT NULL,
    title VARCHAR(1000),
    doi VARCHAR(200),
    url VARCHAR(1000),
    publication_date TIMESTAMP,
    extraction_status VARCHAR(50),
    extracted_text TEXT,
    extraction_error TEXT,
    relevance_score DOUBLE PRECISION,
    relevance_reasoning TEXT,
    supports_gap BOOLEAN,
    conflicts_with_gap BOOLEAN,
    key_findings TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gap_validation_papers_research_gap_id FOREIGN KEY (research_gap_id) REFERENCES research_gaps(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_gap_analyses_paper_id ON gap_analyses(paper_id);
CREATE INDEX IF NOT EXISTS idx_gap_analyses_correlation_id ON gap_analyses(correlation_id);
CREATE INDEX IF NOT EXISTS idx_gap_analyses_request_id ON gap_analyses(request_id);
CREATE INDEX IF NOT EXISTS idx_gap_analyses_status ON gap_analyses(status);
CREATE INDEX IF NOT EXISTS idx_gap_analyses_created_at ON gap_analyses(created_at);

CREATE INDEX IF NOT EXISTS idx_research_gaps_gap_analysis_id ON research_gaps(gap_analysis_id);
CREATE INDEX IF NOT EXISTS idx_research_gaps_gap_id ON research_gaps(gap_id);
CREATE INDEX IF NOT EXISTS idx_research_gaps_validation_status ON research_gaps(validation_status);
CREATE INDEX IF NOT EXISTS idx_research_gaps_category ON research_gaps(category);
CREATE INDEX IF NOT EXISTS idx_research_gaps_estimated_difficulty ON research_gaps(estimated_difficulty);
CREATE INDEX IF NOT EXISTS idx_research_gaps_created_at ON research_gaps(created_at);

CREATE INDEX IF NOT EXISTS idx_gap_validation_papers_research_gap_id ON gap_validation_papers(research_gap_id);
CREATE INDEX IF NOT EXISTS idx_gap_validation_papers_doi ON gap_validation_papers(doi);
CREATE INDEX IF NOT EXISTS idx_gap_validation_papers_relevance_score ON gap_validation_papers(relevance_score);
CREATE INDEX IF NOT EXISTS idx_gap_validation_papers_supports_gap ON gap_validation_papers(supports_gap);
CREATE INDEX IF NOT EXISTS idx_gap_validation_papers_conflicts_with_gap ON gap_validation_papers(conflicts_with_gap);

-- Add comments for documentation
COMMENT ON TABLE gap_analyses IS 'Main gap analysis entity that holds the analysis process and results';
COMMENT ON TABLE research_gaps IS 'Individual research gaps identified in the analysis';
COMMENT ON TABLE gap_validation_papers IS 'Papers analyzed during gap validation';

COMMENT ON COLUMN gap_analyses.paper_id IS 'Reference to the paper being analyzed';
COMMENT ON COLUMN gap_analyses.paper_extraction_id IS 'Reference to the paper extraction used for analysis';
COMMENT ON COLUMN gap_analyses.correlation_id IS 'Unique correlation ID for tracking the analysis request';
COMMENT ON COLUMN gap_analyses.request_id IS 'Unique request ID for tracking the analysis request';
COMMENT ON COLUMN gap_analyses.status IS 'Current status of the gap analysis (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN gap_analyses.config IS 'JSON configuration for the analysis';

COMMENT ON COLUMN research_gaps.gap_analysis_id IS 'Reference to the parent gap analysis';
COMMENT ON COLUMN research_gaps.gap_id IS 'Unique identifier for the gap within the analysis';
COMMENT ON COLUMN research_gaps.category IS 'Category of the gap (theoretical, methodological, empirical, etc.)';
COMMENT ON COLUMN research_gaps.validation_status IS 'Validation status of the gap (INITIAL, VALIDATING, VALID, INVALID, MODIFIED)';
COMMENT ON COLUMN research_gaps.validation_confidence IS 'Confidence score for the gap validation (0-1)';
COMMENT ON COLUMN research_gaps.estimated_difficulty IS 'Estimated difficulty level (low, medium, high)';
COMMENT ON COLUMN research_gaps.evidence_anchors IS 'JSON array of evidence anchors';
COMMENT ON COLUMN research_gaps.suggested_topics IS 'JSON array of suggested research topics';

COMMENT ON COLUMN gap_validation_papers.research_gap_id IS 'Reference to the research gap being validated';
COMMENT ON COLUMN gap_validation_papers.relevance_score IS 'Relevance score of the paper to the gap (0-1)';
COMMENT ON COLUMN gap_validation_papers.supports_gap IS 'Whether this paper supports the gap';
COMMENT ON COLUMN gap_validation_papers.conflicts_with_gap IS 'Whether this paper conflicts with the gap';

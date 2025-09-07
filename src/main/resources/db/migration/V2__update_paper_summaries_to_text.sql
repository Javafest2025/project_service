-- Migration: Update paper_summaries table to use TEXT fields instead of VARCHAR(200)
-- This fixes the "value too long for type character varying(200)" error
-- Hibernate ddl-auto: update doesn't handle column type changes from VARCHAR to TEXT

-- Change VARCHAR fields to TEXT for fields that store AI-generated content
ALTER TABLE paper_summaries
ALTER COLUMN one_liner TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN key_contributions TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN method_overview TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN main_findings TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN limitations TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN applicability TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN research_questions TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN datasets TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN participants TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN procedure_or_pipeline TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN baselines_or_controls TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN metrics TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN statistical_analysis TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN compute_resources TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN implementation_details TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN artifacts TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN reproducibility_notes TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN ethics TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN bias_and_fairness TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN risks_and_misuse TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN data_rights TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN positioning TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN related_works_key TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN impact_notes TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN evidence_anchors TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN threats_to_validity TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN domain_classification TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN technical_depth TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN interdisciplinary_connections TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN future_work TYPE TEXT;

ALTER TABLE paper_summaries
ALTER COLUMN validation_notes TYPE TEXT;

-- Add comment to document the change
COMMENT ON TABLE paper_summaries IS 'Updated to use TEXT fields for AI-generated content to avoid length constraints';

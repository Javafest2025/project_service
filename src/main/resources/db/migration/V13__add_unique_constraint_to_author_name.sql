-- Migration to add unique constraint to author name
-- First, clean up any duplicate authors by keeping the one with the best data quality

-- Create a temporary table to identify duplicates
CREATE TEMP TABLE duplicate_authors AS
SELECT name, COUNT(*) as count
FROM authors
GROUP BY name
HAVING COUNT(*) > 1;

-- For each duplicate name, keep the author with the best data quality
-- (highest data_quality_score, then most recent sync, then highest citation count)
DELETE FROM authors 
WHERE id IN (
    SELECT a.id
    FROM authors a
    INNER JOIN duplicate_authors d ON a.name = d.name
    WHERE a.id NOT IN (
        SELECT DISTINCT ON (a2.name) a2.id
        FROM authors a2
        INNER JOIN duplicate_authors d2 ON a2.name = d2.name
        ORDER BY a2.name, 
                 COALESCE(a2.data_quality_score, 0) DESC,
                 COALESCE(a2.last_sync_at, '1970-01-01'::timestamp) DESC,
                 COALESCE(a2.citation_count, 0) DESC
    )
);

-- Drop the temporary table
DROP TABLE duplicate_authors;

-- Add unique constraint to author name
ALTER TABLE authors ADD CONSTRAINT uk_author_name UNIQUE (name);

-- Create index for case-insensitive name lookups
CREATE INDEX idx_author_name_upper ON authors (UPPER(name));

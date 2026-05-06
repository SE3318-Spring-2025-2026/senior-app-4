ALTER TABLE issue_validation_results
    ADD COLUMN IF NOT EXISTS issue_description TEXT,
    ADD COLUMN IF NOT EXISTS has_review BOOLEAN;

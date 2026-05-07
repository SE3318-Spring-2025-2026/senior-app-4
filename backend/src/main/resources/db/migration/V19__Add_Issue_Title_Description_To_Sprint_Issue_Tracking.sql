ALTER TABLE sprint_issue_tracking
    ADD COLUMN IF NOT EXISTS issue_title       VARCHAR(500),
    ADD COLUMN IF NOT EXISTS issue_description TEXT;

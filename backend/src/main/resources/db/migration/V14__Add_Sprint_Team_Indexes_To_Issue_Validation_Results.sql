ALTER TABLE issue_validation_results
    ADD COLUMN IF NOT EXISTS sprint_id BIGINT,
    ADD COLUMN IF NOT EXISTS team_id   BIGINT;

CREATE INDEX IF NOT EXISTS idx_ivr_sprint_team_issue
    ON issue_validation_results(sprint_id, team_id, issue_key);

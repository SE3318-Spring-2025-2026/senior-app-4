-- Replace (job_id, issue_key) unique constraint with (job_id, team_id, issue_key)
-- so the same issue key from different teams in one sprint job can coexist.
ALTER TABLE issue_validation_results
    DROP CONSTRAINT IF EXISTS issue_validation_results_job_id_issue_key_key;

ALTER TABLE issue_validation_results
    ADD CONSTRAINT issue_validation_results_job_id_team_id_issue_key_key
        UNIQUE (job_id, team_id, issue_key);

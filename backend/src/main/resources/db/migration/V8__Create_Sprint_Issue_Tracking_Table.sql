CREATE TABLE sprint_issue_tracking (
    id                      BIGSERIAL PRIMARY KEY,
    group_id                BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    sprint_id               BIGINT NOT NULL REFERENCES sprint(id) ON DELETE CASCADE,
    issue_key               VARCHAR(50) NOT NULL,
    story_points            INTEGER,
    assignee_github_username VARCHAR(100),
    pr_number               BIGINT,
    pr_merged               BOOLEAN,
    synced_at               TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_sprint_issue UNIQUE (group_id, sprint_id, issue_key)
);

CREATE INDEX idx_sit_group_sprint ON sprint_issue_tracking(group_id, sprint_id);
CREATE INDEX idx_sit_issue_key ON sprint_issue_tracking(issue_key);

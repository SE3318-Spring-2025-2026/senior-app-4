CREATE TABLE IF NOT EXISTS issue_validation_results (
    id                        BIGSERIAL PRIMARY KEY,
    job_id                    BIGINT        NOT NULL REFERENCES validation_jobs(job_id),
    issue_key                 VARCHAR(64)   NOT NULL,
    issue_title               TEXT,
    assignee                  VARCHAR(255),
    pr_number                 BIGINT,
    pr_url                    TEXT,
    pr_merged                 BOOLEAN,

    review_score              NUMERIC(5,2),
    review_quality            VARCHAR(16),
    reviewer_count            INT,
    has_change_requests       BOOLEAN,
    has_substantive_comments  BOOLEAN,
    review_ai_feedback        TEXT,

    impl_score                NUMERIC(5,2),
    impl_is_valid             BOOLEAN,
    impl_missing_requirements TEXT,
    impl_coverage_areas       TEXT,
    impl_ai_feedback          TEXT,
    files_analyzed            INT,
    diff_truncated            BOOLEAN,

    composite_score           NUMERIC(5,2),
    validation_status         VARCHAR(16)   NOT NULL,
    evaluated_at              TIMESTAMP WITH TIME ZONE NOT NULL,

    UNIQUE (job_id, issue_key)
);

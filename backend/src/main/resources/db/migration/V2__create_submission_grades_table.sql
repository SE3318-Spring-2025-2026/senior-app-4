CREATE TABLE IF NOT EXISTS submission_grades (
    grade_id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    comments TEXT,
    graded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

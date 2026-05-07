CREATE TABLE grade_criterion_scores (
    id           BIGSERIAL PRIMARY KEY,
    grade_id     BIGINT         NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    criterion_id BIGINT         NOT NULL REFERENCES grading_criteria(id) ON DELETE RESTRICT,
    score        DOUBLE PRECISION NOT NULL,
    UNIQUE (grade_id, criterion_id)
);

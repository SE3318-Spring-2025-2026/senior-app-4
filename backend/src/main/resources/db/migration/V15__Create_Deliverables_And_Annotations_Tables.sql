-- Deliverables table for submission tracking
CREATE TABLE IF NOT EXISTS deliverables (
    id                   BIGSERIAL PRIMARY KEY,
    group_id             BIGINT        NOT NULL,
    deliverable_type     VARCHAR(50)   NOT NULL CHECK (deliverable_type IN (
                             'PROPOSAL', 'REVISED_PROPOSAL', 'STATEMENT_OF_WORK', 'DEMONSTRATION'
                         )),
    content              TEXT          NOT NULL DEFAULT '',
    file_url             VARCHAR(512),
    status               VARCHAR(50)   NOT NULL CHECK (status IN (
                             'SUBMITTED', 'PENDING_REVIEW', 'UNDER_REVIEW',
                             'REVISION_REQUESTED', 'APPROVED', 'GRADED', 'SUPERSEDED', 'REJECTED'
                         )),
    committee_id         BIGINT,
    final_grade          DOUBLE PRECISION,
    parent_submission_id BIGINT        REFERENCES deliverables(id) ON DELETE SET NULL,
    version              INTEGER       NOT NULL DEFAULT 1,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_deliverables_group_id        ON deliverables(group_id);
CREATE INDEX IF NOT EXISTS idx_deliverables_committee_id    ON deliverables(committee_id);
CREATE INDEX IF NOT EXISTS idx_deliverables_status          ON deliverables(status);
CREATE INDEX IF NOT EXISTS idx_deliverables_parent          ON deliverables(parent_submission_id);

-- Submission annotations: advisor selects text in markdown and links it to a grading criterion
CREATE TABLE IF NOT EXISTS submission_annotations (
    id               BIGSERIAL PRIMARY KEY,
    submission_id    BIGINT        NOT NULL REFERENCES deliverables(id) ON DELETE CASCADE,
    advisor_id       BIGINT        NOT NULL,
    criterion_id     BIGINT,
    selected_text    TEXT          NOT NULL,
    start_offset     INTEGER       NOT NULL,
    end_offset       INTEGER       NOT NULL,
    comment          TEXT,
    grade            VARCHAR(10),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_annotations_submission_id ON submission_annotations(submission_id);
CREATE INDEX IF NOT EXISTS idx_annotations_advisor_id    ON submission_annotations(advisor_id);
CREATE INDEX IF NOT EXISTS idx_annotations_criterion_id  ON submission_annotations(criterion_id);

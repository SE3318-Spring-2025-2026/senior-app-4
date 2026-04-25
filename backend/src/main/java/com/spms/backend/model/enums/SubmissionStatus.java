package com.spms.backend.model.enums;

public enum SubmissionStatus {
    PENDING_REVIEW,
    REVIEWING,           // spec calls this UNDER_REVIEW — kept as REVIEWING to avoid breaking other branch merges
    REVISION_REQUESTED,
    APPROVED,
    GRADED,
    SUPERSEDED           // internal status: set on parent when a revision is submitted
}

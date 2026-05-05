package com.spms.backend.model;

public enum ValidationJobStep {
    LOADING_CONTEXT,
    FETCHING_PR_DETAILS,
    FETCHING_DIFFS,
    AI_REVIEW_VERIFICATION,
    AI_IMPLEMENTATION_VALIDATION,
    STORING_RESULTS
}

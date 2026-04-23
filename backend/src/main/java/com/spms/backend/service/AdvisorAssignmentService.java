package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;

public interface AdvisorAssignmentService {

    /**
     * P4-ASSIGN-1 / #160 — list group–advisor mappings.
     * Coordinators: system-wide (optional {@code advisorId}, {@code hasAdvisor}).
     * Professors: only groups they advise.
     */
    AdvisorAssignmentListResponse listAdvisorAssignments(
            String requesterRole, Long requesterUserId, Long filterAdvisorId, Boolean hasAdvisor);
}

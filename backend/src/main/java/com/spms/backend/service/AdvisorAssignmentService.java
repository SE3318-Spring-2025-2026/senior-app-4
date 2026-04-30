package com.spms.backend.service;

import com.spms.backend.dto.response.AdvisorAssignmentListResponse;

public interface AdvisorAssignmentService {

    void releaseAdvisor(Long groupId, Long professorId, String role);

    /**
     * P4-ASSIGN-1 / #160 — list group–advisor mappings.
     * Coordinators: system-wide (optional {@code advisorId}, {@code hasAdvisor}).
     * Professors: only groups they advise.
     */
    AdvisorAssignmentListResponse listAdvisorAssignments(
            String requesterRole, Long requesterUserId, Long filterAdvisorId, Boolean hasAdvisor);

    void assignAdvisor(Long committeeId, Long advisorId, String role, Long assignedBy);
    
    void removeAdvisor(Long committeeId, Long advisorId);
}

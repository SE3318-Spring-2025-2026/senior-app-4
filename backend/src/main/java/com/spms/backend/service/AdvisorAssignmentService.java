package com.spms.backend.service;

public interface AdvisorAssignmentService {

    void releaseAdvisor(Long groupId, Long professorId, String role);
}

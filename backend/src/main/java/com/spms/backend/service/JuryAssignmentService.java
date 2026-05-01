package com.spms.backend.service;

import com.spms.backend.dto.response.JuryListResponse;

public interface JuryAssignmentService {

    void assignJury(Long committeeId, Long juryMemberId, String juryType, Long assignedBy);

    void removeJury(Long committeeId, Long juryMemberId);

    JuryListResponse getJuryByCommittee(Long committeeId);

    JuryListResponse getCommitteesByJury(Long juryMemberId);
}

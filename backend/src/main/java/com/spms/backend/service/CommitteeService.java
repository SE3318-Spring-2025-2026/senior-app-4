package com.spms.backend.service;

import com.spms.backend.model.Committee;
import com.spms.backend.dto.request.CommitteeCreateRequest;

import java.util.List;

public interface CommitteeService {
    Committee createCommittee(CommitteeCreateRequest request, Long coordinatorId);
    Committee updateCommittee(Long id, CommitteeCreateRequest request, Long coordinatorId);
    void deleteCommittee(Long id, Long coordinatorId);
    Committee getCommitteeById(Long id);
    List<Committee> getAllCommittees();
}

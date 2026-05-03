package com.spms.backend.service;

import com.spms.backend.model.Committee;
import com.spms.backend.dto.request.CommitteeCreateRequest;
import com.spms.backend.model.enums.CommitteeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommitteeService {
    Committee createCommittee(CommitteeCreateRequest request, Long coordinatorId);
    Committee updateCommittee(Long id, CommitteeCreateRequest request, Long coordinatorId);
    void deleteCommittee(Long id, Long coordinatorId);
    Committee getCommitteeById(Long id);
    Committee getCommitteeByIdWithFullDetails(Long id);
    List<Committee> getAllCommittees();
    Page<Committee> getAllCommitteesPaginated(Pageable pageable);
    Page<Committee> getCommitteesByStatus(CommitteeStatus status, Pageable pageable);
    Page<Committee> getCommitteesByCoordinator(Long coordinatorId, Pageable pageable);
    Page<Committee> getCommitteesByFilters(CommitteeStatus status, String search, String sort,Pageable pageable); //komite search bar ve filtreleme için eklendi
}

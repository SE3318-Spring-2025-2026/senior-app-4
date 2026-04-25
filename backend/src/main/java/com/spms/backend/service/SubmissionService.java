package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.data.domain.Pageable;

public interface SubmissionService {

    SubmissionResponse submit(Long groupId, DeliverableType type,
                              String content, String fileName, Long callerId);

    SubmissionListResponse listSubmissions(Long userId, String role, Pageable pageable);

    /**
     * POST /submissions/{submissionId}/revisions  [P3-REV-1]
     * Parent must be in REVISION_REQUESTED status; parent is updated to SUPERSEDED;
     * version auto-increments from parent version.
     */
    RevisionCreateResponseDto createRevision(Long parentSubmissionId,
                                             String fileName,
                                             String description,
                                             Long callerId);

    /**
     * GET /submissions/{submissionId}/revisions  [P3-REV-2]
     * Returns the full revision chain including the root submission.
     */
    RevisionHistoryResponseDto getRevisionHistory(Long submissionId);
}

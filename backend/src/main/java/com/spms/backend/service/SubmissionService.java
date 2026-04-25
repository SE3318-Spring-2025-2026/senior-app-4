package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for submission-related business logic.
 * Implementation: {@link com.spms.backend.service.impl.SubmissionServiceImpl}
 */
public interface SubmissionService {

    /**
     * Submit a new deliverable on behalf of a group leader.
     * Validates pipeline order, committee assignment, and leader identity.
     */
    SubmissionResponse submit(Long groupId, DeliverableType type, String content, String fileName, Long callerId);

    /**
     * List submissions filtered by the caller's role.
     * COORDINATORs see all; STUDENTs see only their group's submissions.
     */
    SubmissionListResponse listSubmissions(Long userId, String role, Pageable pageable);

    /**
     * [P3-REV-1] Submit a revised version of a deliverable.
     * Parent must be in REVISION_REQUESTED status.
     * Caller must be the group leader.
     * Creates a new Submission linked via parentSubmissionId with an incremented version.
     * Sets parent status to SUPERSEDED.
     */
    RevisionCreateResponseDto createRevision(Long parentSubmissionId, Long callerId, MultipartFile file, String description);

    /**
     * [P3-REV-2] Return the full revision chain for a submission.
     * Traverses to the root and walks all children, ordered by version.
     * Includes the original submission in the result.
     * Throws NotFoundException if submissionId does not exist.
     */
    RevisionHistoryResponseDto getRevisionHistory(Long submissionId);
}

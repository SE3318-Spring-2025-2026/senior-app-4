package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionDetailResponse;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface SubmissionService {

    SubmissionResponse submit(Long groupId, DeliverableType type,
                              String content, MultipartFile file, Long callerId);

    SubmissionListResponse listSubmissions(Long userId, String role,
                                           Long teamId, String status,
                                           Long committeeId, String deliverableType,
                                           String deadlineStatus,
                                           Pageable pageable);

    SubmissionDetailResponse getSubmission(Long submissionId, Long userId, String role);

    RevisionCreateResponseDto createRevision(Long parentSubmissionId,
                                             MultipartFile file,
                                             String description,
                                             Long callerId);

    RevisionHistoryResponseDto getRevisionHistory(Long submissionId, Long userId, String role);
}

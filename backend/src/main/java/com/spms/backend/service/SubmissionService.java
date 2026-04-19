package com.spms.backend.service;

import com.spms.backend.dto.SubmissionRequest;
import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public SubmissionResponse submit(SubmissionRequest request) {
        
        // Pipeline Validation: SoW requires Proposal grading completion.
        if (request.getType() == DeliverableType.SOW) {
            Optional<Submission> previousProposal = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(request.getGroupId(), DeliverableType.PROPOSAL);

            if (previousProposal.isEmpty() || previousProposal.get().getStatus() != SubmissionStatus.GRADED) {
                throw new IllegalStateException("Cannot submit SoW: The Proposal for this group must be fully GRADED first.");
            }
        }

        Submission submission = new Submission();
        submission.setGroupId(request.getGroupId());
        submission.setDeliverableType(request.getType());
        submission.setContent(request.getContent());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        
        // TODO: Fetch group's advisor and assign to the correct committee (Process 2 Integration)
        // Since Group and Committee entities belong to Process 2, we mock the committee assignment for now.
        // Long assignedCommitteeId = process2CommitteeService.getCommitteeForGroup(request.getGroupId());
        Long mockCommitteeId = 1L; 
        submission.setCommitteeId(mockCommitteeId);

        Submission savedSubmission = submissionRepository.save(submission);

        // TODO: Trigger notification for committee members and coordinator (D8) (Process 2/Notification Integration)
        // notificationService.sendSubmissionNotification(savedSubmission);

        return new SubmissionResponse(
            "Submission successfully created.", 
            savedSubmission.getId(), 
            savedSubmission.getStatus()
        );
    }
}

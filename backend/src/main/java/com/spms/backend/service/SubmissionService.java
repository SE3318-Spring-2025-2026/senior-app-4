package com.spms.backend.service;

import com.spms.backend.dto.SubmissionRequest;
import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    public SubmissionService(SubmissionRepository submissionRepository, 
                             GroupRepository groupRepository,
                             GroupCommitteeAssignmentRepository assignmentRepository,
                             NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
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
        
        // Process 2 Integration: Fetch group and assign committee
        Optional<Group> groupOpt = groupRepository.findById(request.getGroupId());
        if (groupOpt.isPresent()) {
            Group group = groupOpt.get();
            
            // Auto-assign Committee
            Optional<GroupCommitteeAssignment> assignmentOpt = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(group.getId(), "ASSIGNED");
            
            if (assignmentOpt.isPresent()) {
                submission.setCommitteeId(assignmentOpt.get().getCommittee().getCommitteeId());
            }

            // Save submission first
            Submission savedSubmission = submissionRepository.save(submission);

            // Process 2 Integration: Trigger notification to advisor (and implicitly coordinator/committee members)
            if (group.getAdvisor() != null) {
                String message = "A new " + request.getType() + " has been submitted by Group: " + group.getGroupName();
                notificationService.createSystemAlert(
                    group.getAdvisor().getUserId(), 
                    message, 
                    "SUBMISSION_ALERT", 
                    "submissionId:" + savedSubmission.getId()
                );
            }

            return new SubmissionResponse(
                "Submission successfully created.", 
                savedSubmission.getId(), 
                savedSubmission.getStatus()
            );
        } else {
            throw new IllegalArgumentException("Invalid group ID provided.");
        }
    }
}

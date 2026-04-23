package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public SubmissionService(SubmissionRepository submissionRepository, 
                             GroupRepository groupRepository,
                             GroupCommitteeAssignmentRepository assignmentRepository,
                             NotificationService notificationService,
                             UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public SubmissionResponse submit(Long groupId, DeliverableType type, String content, String fileName, Long callerId) {
        
        // FIX-4: Authorization - Check if group exists and caller is leader
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit.");
        }

        // FIX-8: Forbidden if no active committee assignment
        GroupCommitteeAssignment assignment = assignmentRepository
            .findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED")
            .orElseThrow(() -> new ForbiddenException("Group is not assigned to any committee."));

        // FIX-7: Pipeline Validation
        if (type == DeliverableType.STATEMENT_OF_WORK) {
            Optional<Submission> previousProposal = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);

            if (previousProposal.isEmpty() || previousProposal.get().getStatus() != SubmissionStatus.GRADED) {
                throw new BadRequestException("Cannot submit Statement of Work: The Proposal for this group must be fully GRADED first.");
            }
        } else if (type == DeliverableType.REVISED_PROPOSAL) {
            Optional<Submission> originalProposal = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);
            
            if (originalProposal.isEmpty() || originalProposal.get().getStatus() != SubmissionStatus.REVISION_REQUESTED) {
                throw new BadRequestException("Cannot submit Revised Proposal: An existing Proposal must have REVISION_REQUESTED status.");
            }
        }

        // Create Submission
        Submission submission = new Submission();
        submission.setGroupId(groupId);
        submission.setDeliverableType(type);
        submission.setContent(content);
        submission.setFileUrl("/uploads/" + fileName); // FIX-3: Store mock file reference
        submission.setStatus(SubmissionStatus.PENDING_REVIEW); // FIX-2: Initial status
        submission.setCommitteeId(assignment.getCommittee().getCommitteeId());
        
        Submission savedSubmission = submissionRepository.save(submission);

        // FIX-5: Notify committee members and coordinator
        Committee committee = assignment.getCommittee();
        String notificationMsg = "New " + type + " submitted by Group: " + group.getGroupName();
        
        // Notify Advisors in Committee
        if (committee.getAdvisors() != null) {
            committee.getAdvisors().forEach(advisor -> 
                notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
            );
        }
        
        // Notify Jury Members in Committee
        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().forEach(jury -> 
                notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
            );
        }

        // Notify Coordinators
        userRepository.findAllByRole("COORDINATOR").forEach(coordinator ->
            notificationService.createSystemAlert(coordinator.getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
        );

        // FIX-6: Map to structured response
        SubmissionResponse.SubmissionData data = new SubmissionResponse.SubmissionData(
            savedSubmission.getId(),
            savedSubmission.getGroupId(),
            savedSubmission.getDeliverableType(),
            savedSubmission.getStatus(),
            savedSubmission.getCommitteeId(),
            savedSubmission.getCreatedAt()
        );

        return new SubmissionResponse("success", "Submission successfully created.", data);
    }
}

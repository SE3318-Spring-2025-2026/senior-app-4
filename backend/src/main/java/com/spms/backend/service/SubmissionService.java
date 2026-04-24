package com.spms.backend.service;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.SubmissionListResponse;
import com.spms.backend.dto.response.SubmissionListResponse.PaginationMeta;
import com.spms.backend.dto.response.SubmissionListResponse.SubmissionSummary;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.Group;
import com.spms.backend.model.GroupCommitteeAssignment;
import com.spms.backend.model.GroupMember;
import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public SubmissionService(SubmissionRepository submissionRepository, 
                             GroupRepository groupRepository,
                             GroupCommitteeAssignmentRepository assignmentRepository,
                             NotificationService notificationService,
                             UserRepository userRepository,
                             GroupMemberRepository groupMemberRepository) {
        this.submissionRepository = submissionRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Transactional
    public SubmissionResponse submit(Long groupId, DeliverableType type, String content, String fileName, Long callerId) {
        
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit.");
        }

        GroupCommitteeAssignment assignment = assignmentRepository
            .findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED")
            .orElseThrow(() -> new ForbiddenException("Group is not assigned to any committee."));

        // Pipeline Validation
        if (type == DeliverableType.SOW || type == DeliverableType.STATEMENT_OF_WORK) {
            Optional<Submission> previousProposal = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);

            if (previousProposal.isEmpty() || previousProposal.get().getStatus() != SubmissionStatus.GRADED) {
                throw new BadRequestException("Cannot submit Statement of Work: The Proposal for this group must be fully GRADED first.");
            }
        } else if (type == DeliverableType.REVISED_PROPOSAL || type == DeliverableType.REVISION) {
            Optional<Submission> originalProposal = submissionRepository
                .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);
            
            if (originalProposal.isEmpty() || originalProposal.get().getStatus() != SubmissionStatus.REVISION_REQUESTED) {
                throw new BadRequestException("Cannot submit Revised Proposal: An existing Proposal must have REVISION_REQUESTED status.");
            }
        }

        Submission submission = new Submission();
        submission.setGroupId(groupId);
        submission.setDeliverableType(type);
        submission.setContent(content);
        submission.setFileUrl("/uploads/" + fileName); 
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setCommitteeId(assignment.getCommittee().getCommitteeId());
        
        Submission savedSubmission = submissionRepository.save(submission);

        Committee committee = assignment.getCommittee();
        String notificationMsg = "New " + type + " submitted by Group: " + group.getGroupName();
        
        if (committee.getAdvisors() != null) {
            committee.getAdvisors().forEach(advisor -> 
                notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
            );
        }
        
        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().forEach(jury -> 
                notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
            );
        }

        userRepository.findAllByRole("COORDINATOR").forEach(coordinator ->
            notificationService.createSystemAlert(coordinator.getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId())
        );

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

    @Transactional(readOnly = true)
    public SubmissionListResponse listSubmissions(Long userId, String role, Pageable pageable) {
        Page<Submission> page;

        if ("COORDINATOR".equalsIgnoreCase(role)) {
            page = submissionRepository.findAll(pageable);
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException(
                            "Student is not assigned to any group."));
            page = submissionRepository.findByGroupId(member.getGroup().getId(), pageable);
        } else {
            throw new ForbiddenException("Role '" + role + "' is not authorized to list submissions.");
        }

        List<SubmissionSummary> items = page.getContent().stream()
                .map(s -> new SubmissionSummary(
                        s.getId(),
                        s.getGroupId(),
                        s.getDeliverableType() != null ? s.getDeliverableType().name() : null,
                        s.getStatus() != null ? s.getStatus().name() : null,
                        s.getCommitteeId(),
                        s.getCreatedAt()))
                .toList();

        PaginationMeta meta = new PaginationMeta(
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());

        return new SubmissionListResponse("success", items, meta);
    }
}

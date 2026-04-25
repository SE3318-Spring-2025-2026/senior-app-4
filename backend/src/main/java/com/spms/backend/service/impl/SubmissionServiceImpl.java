package com.spms.backend.service.impl;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto.RevisionHistoryData;
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
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
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

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public SubmissionListResponse listSubmissions(Long userId, String role, Pageable pageable) {
        Page<Submission> page;

        if ("COORDINATOR".equalsIgnoreCase(role)) {
            page = submissionRepository.findAll(pageable);
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("Student is not assigned to any group."));
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

    /**
     * [P3-REV-1] Creates a revision linked to the parent submission.
     *
     * <ul>
     *   <li>Parent must be in REVISION_REQUESTED status (400 otherwise).</li>
     *   <li>Caller must be the group leader (403 otherwise).</li>
     *   <li>Parent status is set to SUPERSEDED.</li>
     *   <li>New revision gets version = parent.version + 1.</li>
     * </ul>
     */
    @Override
    @Transactional
    public RevisionCreateResponseDto createRevision(Long parentSubmissionId, Long callerId, MultipartFile file, String description) {
        // 404 — parent not found
        Submission parent = submissionRepository.findById(parentSubmissionId)
                .orElseThrow(() -> new NotFoundException("Parent submission not found with ID: " + parentSubmissionId));

        // 400 — parent not in REVISION_REQUESTED
        if (parent.getStatus() != SubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException(
                    "Parent submission is not in REVISION_REQUESTED status. Current status: " + parent.getStatus());
        }

        // 403 — caller must be group leader
        Group group = groupRepository.findById(parent.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found for submission."));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit revisions.");
        }

        // Mark parent as superseded
        parent.setStatus(SubmissionStatus.SUPERSEDED);
        submissionRepository.save(parent);

        // Build revision
        Submission revision = new Submission();
        revision.setGroupId(parent.getGroupId());
        revision.setDeliverableType(parent.getDeliverableType());
        revision.setContent(description != null ? description : "");
        revision.setFileUrl("/uploads/" + file.getOriginalFilename());
        revision.setStatus(SubmissionStatus.PENDING_REVIEW);
        revision.setCommitteeId(parent.getCommitteeId());
        revision.setParentSubmissionId(parentSubmissionId);
        revision.setVersion(parent.getVersion() + 1);

        Submission savedRevision = submissionRepository.save(revision);

        // Notify committee members
        Committee committee = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(parent.getGroupId(), "ASSIGNED")
                .map(GroupCommitteeAssignment::getCommittee)
                .orElse(null);

        if (committee != null) {
            String notificationMsg = "Revised " + parent.getDeliverableType() + " submitted by Group: " + group.getGroupName();
            if (committee.getAdvisors() != null) {
                committee.getAdvisors().forEach(advisor ->
                        notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedRevision.getId())
                );
            }
            if (committee.getJuryMembers() != null) {
                committee.getJuryMembers().forEach(jury ->
                        notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg, "SUBMISSION_ALERT", "submissionId:" + savedRevision.getId())
                );
            }
        }

        RevisionCreateResponseDto.RevisionData data = new RevisionCreateResponseDto.RevisionData(
                savedRevision.getId(),
                savedRevision.getParentSubmissionId(),
                savedRevision.getVersion(),
                savedRevision.getStatus().name(),
                savedRevision.getCreatedAt()
        );

        return new RevisionCreateResponseDto("success", "Revision submitted successfully.", data);
    }

    /**
     * [P3-REV-2] Returns the full revision chain for a submission.
     *
     * <p>Traverses upward to find the root (original) submission, then walks
     * downward through all children. Includes the original submission itself.
     * Throws {@link NotFoundException} when the given submissionId does not exist.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public RevisionHistoryResponseDto getRevisionHistory(Long submissionId) {
        // 404 guard — throw if submission doesn't exist
        Submission current = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + submissionId));

        // Walk up to root
        Submission root = current;
        while (root.getParentSubmissionId() != null) {
            Long parentId = root.getParentSubmissionId();
            root = submissionRepository.findById(parentId)
                    .orElseThrow(() -> new NotFoundException("Broken revision chain: parent submission not found."));
        }

        // Walk down from root to build full chain
        List<Submission> chain = new ArrayList<>();
        chain.add(root);

        List<Submission> children = submissionRepository.findByParentSubmissionIdOrderByIdAsc(root.getId());
        while (!children.isEmpty()) {
            Submission child = children.get(0);
            chain.add(child);
            children = submissionRepository.findByParentSubmissionIdOrderByIdAsc(child.getId());
        }

        List<RevisionHistoryData> historyData = chain.stream()
                .map(s -> new RevisionHistoryData(
                        s.getId(),
                        s.getVersion(),
                        s.getStatus().name(),
                        s.getCreatedAt(),
                        s.getContent()
                )).toList();

        return new RevisionHistoryResponseDto("success", historyData);
    }
}

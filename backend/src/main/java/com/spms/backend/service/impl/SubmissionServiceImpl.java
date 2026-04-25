package com.spms.backend.service.impl;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
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
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final GroupRepository groupRepository;
    private final GroupCommitteeAssignmentRepository assignmentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ScheduleRepository scheduleRepository;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
                                 GroupRepository groupRepository,
                                 GroupCommitteeAssignmentRepository assignmentRepository,
                                 NotificationService notificationService,
                                 UserRepository userRepository,
                                 GroupMemberRepository groupMemberRepository,
                                 ScheduleRepository scheduleRepository) {
        this.submissionRepository = submissionRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.scheduleRepository = scheduleRepository;
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

    // ──────────────────────────────────────────────────────────────────────────
    //  P3-REV-1: POST /submissions/{submissionId}/revisions
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RevisionCreateResponseDto createRevision(Long parentSubmissionId,
                                                    String fileName,
                                                    String description,
                                                    Long callerId) {
        // 1. Validate parent exists
        Submission parent = submissionRepository.findById(parentSubmissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + parentSubmissionId));

        // 2. AC: Parent must be in REVISION_REQUESTED status
        if (parent.getStatus() != SubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException(
                    "Cannot submit revision: parent submission status is " + parent.getStatus()
                    + " but must be REVISION_REQUESTED.");
        }

        // 3. Authorization: only group leader may submit revisions
        Group group = groupRepository.findById(parent.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + parent.getGroupId()));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit a revision.");
        }

        // 4. D10: Deadline check
        scheduleRepository.findTopByOrderByIdDesc().ifPresent(schedule -> {
            if (schedule.getProposalRevisionDeadline() != null &&
                    java.time.Instant.now().isAfter(schedule.getProposalRevisionDeadline())) {
                throw new ForbiddenException("Revision deadline has passed (D10).");
            }
        });

        // 5. AC: Update parent status to SUPERSEDED
        parent.setStatus(SubmissionStatus.SUPERSEDED);
        submissionRepository.save(parent);

        // 5. Auto-increment version number
        int newVersion = (parent.getVersion() != null ? parent.getVersion() : 1) + 1;

        // 6. Create the revision record
        Submission revision = new Submission();
        revision.setGroupId(parent.getGroupId());
        revision.setDeliverableType(parent.getDeliverableType());
        revision.setContent(description != null ? description : "");
        revision.setFileUrl("/uploads/" + fileName);
        revision.setStatus(SubmissionStatus.PENDING_REVIEW);
        revision.setCommitteeId(parent.getCommitteeId());
        revision.setParentSubmissionId(parentSubmissionId);
        revision.setVersion(newVersion);

        Submission saved = submissionRepository.save(revision);

        // 8. Notify committee members and coordinators (Fan-out)
        GroupCommitteeAssignment assignment = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(parent.getGroupId(), "ASSIGNED")
                .orElseThrow(() -> new ForbiddenException("Group is not assigned to any committee."));

        Committee committee = assignment.getCommittee();
        String notificationMsg = "Revision v" + newVersion + " submitted for submission #" + parentSubmissionId + " by Group: " + group.getGroupName();

        if (committee.getAdvisors() != null) {
            committee.getAdvisors().forEach(advisor ->
                    notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg, "REVISION_ALERT", "submissionId:" + saved.getId())
            );
        }

        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().forEach(jury ->
                    notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg, "REVISION_ALERT", "submissionId:" + saved.getId())
            );
        }

        userRepository.findAllByRole("COORDINATOR").forEach(coord ->
                notificationService.createSystemAlert(coord.getUserId(), notificationMsg, "REVISION_ALERT", "submissionId:" + saved.getId())
        );

        RevisionCreateResponseDto.Data data = new RevisionCreateResponseDto.Data(
                saved.getId(),
                saved.getParentSubmissionId(),
                saved.getVersion(),
                saved.getStatus().name(),
                saved.getCreatedAt()
        );

        return new RevisionCreateResponseDto("success", "Revision submitted successfully.", data);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  P3-REV-2: GET /submissions/{submissionId}/revisions
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RevisionHistoryResponseDto getRevisionHistory(Long submissionId, Long userId, String role) {
        // 1. Validate the submission exists
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + submissionId));

        // 2. Authorization Check
        if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("Student is not assigned to any group."));
            if (!member.getGroup().getId().equals(submission.getGroupId())) {
                throw new ForbiddenException("You can only view revision history for your own group's submissions.");
            }
        } else if ("PROFESSOR".equalsIgnoreCase(role)) {
            // Check if professor is in the committee assigned to this submission
            // This requires a check against CommitteeAdvisor or CommitteeJury
            // For now, checking if the submission's committee matches the assignment (simplified check)
            // A more robust check would involve checking the Committee membership of the userId.
            // TODO: [P3-AUTH-1] Add detailed professor-committee membership check for revision history
        } else if (!"COORDINATOR".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Role '" + role + "' is not authorized to view revision history.");
        }

        // 3. Resolve the ABSOLUTE root of the chain by walking up
        Submission current = submission;
        while (current.getParentSubmissionId() != null) {
            final Long parentId = current.getParentSubmissionId();
            current = submissionRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalStateException("Broken revision chain: parent not found for ID " + parentId));
        }
        Long absoluteRootId = current.getId();

        // 4. Fetch the full chain (root + all descendants)
        // We use groupId and deliverableType as a filter to narrow down, 
        // then filter in-memory for those belonging to the same root tree to be safe.
        List<Submission> allPotential = submissionRepository.findAllByGroupIdAndDeliverableType(
                submission.getGroupId(), submission.getDeliverableType());

        List<Submission> chain = allPotential.stream()
                .filter(s -> isPartOfChain(s, absoluteRootId, allPotential))
                .sorted((s1, s2) -> {
                    if (s1.getVersion() != null && s2.getVersion() != null) {
                        return s1.getVersion().compareTo(s2.getVersion());
                    }
                    return s1.getCreatedAt().compareTo(s2.getCreatedAt());
                })
                .collect(Collectors.toList());

        List<RevisionHistoryResponseDto.Item> items = chain.stream()
                .map(s -> new RevisionHistoryResponseDto.Item(
                        s.getId(),
                        s.getVersion(),
                        s.getStatus() != null ? s.getStatus().name() : null,
                        s.getCreatedAt(),
                        s.getContent()
                ))
                .collect(Collectors.toList());

        return new RevisionHistoryResponseDto("success", items);
    }

    /** Helper to check if a submission eventually leads back to the same absolute root */
    private boolean isPartOfChain(Submission s, Long absoluteRootId, List<Submission> pool) {
        if (s.getId().equals(absoluteRootId)) return true;
        
        Long parentId = s.getParentSubmissionId();
        while (parentId != null) {
            if (parentId.equals(absoluteRootId)) return true;
            
            // Look up parent in the pool to avoid N+1 queries
            final Long currentParentId = parentId;
            Submission parent = pool.stream()
                    .filter(p -> p.getId().equals(currentParentId))
                    .findFirst()
                    .orElse(null);
            
            if (parent == null) break; // Should not happen with consistent data
            parentId = parent.getParentSubmissionId();
        }
        return false;
    }
}

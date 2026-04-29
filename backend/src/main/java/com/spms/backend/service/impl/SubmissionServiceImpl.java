package com.spms.backend.service.impl;

import com.spms.backend.dto.SubmissionResponse;
import com.spms.backend.dto.response.RevisionCreateResponseDto;
import com.spms.backend.dto.response.RevisionHistoryResponseDto;
import com.spms.backend.dto.response.SubmissionDetailResponse;
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
import com.spms.backend.model.Schedule;
import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.CommitteeAdvisorRepository;
import com.spms.backend.repository.GroupCommitteeAssignmentRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.ScheduleRepository;
import com.spms.backend.repository.SubmissionRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.service.FileStorageService;
import com.spms.backend.service.NotificationService;
import com.spms.backend.service.SubmissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
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
    private final FileStorageService fileStorageService;
    private final CommitteeAdvisorRepository committeeAdvisorRepository;

    public SubmissionServiceImpl(SubmissionRepository submissionRepository,
                                 GroupRepository groupRepository,
                                 GroupCommitteeAssignmentRepository assignmentRepository,
                                 NotificationService notificationService,
                                 UserRepository userRepository,
                                 GroupMemberRepository groupMemberRepository,
                                 ScheduleRepository scheduleRepository,
                                 FileStorageService fileStorageService,
                                 CommitteeAdvisorRepository committeeAdvisorRepository) {
        this.submissionRepository = submissionRepository;
        this.groupRepository = groupRepository;
        this.assignmentRepository = assignmentRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.scheduleRepository = scheduleRepository;
        this.fileStorageService = fileStorageService;
        this.committeeAdvisorRepository = committeeAdvisorRepository;
    }

    @Override
    @Transactional
    public SubmissionResponse submit(Long groupId, DeliverableType type,
                                     String content, MultipartFile file, Long callerId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + groupId));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit.");
        }

        GroupCommitteeAssignment assignment = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(groupId, "ASSIGNED")
                .orElseThrow(() -> new ForbiddenException("Group is not assigned to any committee."));

        // C-6: TODO(P2-dep): submission deadline check against D11 Schedule needs
        // proposal/SoW/demo deadline columns not yet in Schedule entity — touches P2 boundary.
        // Implement once P2 team adds those columns to the schedule table.

        // C-7: Pipeline validation (cleaned up after B-2 enum consolidation)
        if (type == DeliverableType.STATEMENT_OF_WORK) {
            Optional<Submission> previousProposal = submissionRepository
                    .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);
            if (previousProposal.isEmpty() || previousProposal.get().getStatus() != SubmissionStatus.GRADED) {
                throw new BadRequestException("Cannot submit Statement of Work: Proposal must be GRADED first.");
            }
        } else if (type == DeliverableType.REVISED_PROPOSAL) {
            Optional<Submission> originalProposal = submissionRepository
                    .findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(groupId, DeliverableType.PROPOSAL);
            if (originalProposal.isEmpty() || originalProposal.get().getStatus() != SubmissionStatus.REVISION_REQUESTED) {
                throw new BadRequestException("Cannot submit Revised Proposal: Proposal must have REVISION_REQUESTED status.");
            }
        }

        // C-8: store the actual file
        String fileUrl = fileStorageService.store(file);

        Submission submission = new Submission();
        submission.setGroupId(groupId);
        submission.setDeliverableType(type);
        submission.setContent(content);
        submission.setFileUrl(fileUrl);
        submission.setStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setCommitteeId(assignment.getCommittee().getCommitteeId());

        Submission savedSubmission = submissionRepository.save(submission);

        Committee committee = assignment.getCommittee();
        String notificationMsg = "New " + type + " submitted by Group: " + group.getGroupName();

        if (committee.getAdvisors() != null) {
            committee.getAdvisors().forEach(advisor ->
                    notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg,
                            "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId()));
        }
        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().forEach(jury ->
                    notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg,
                            "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId()));
        }
        userRepository.findAllByRoleIgnoreCase("COORDINATOR").forEach(coordinator ->
                notificationService.createSystemAlert(coordinator.getUserId(), notificationMsg,
                        "SUBMISSION_ALERT", "submissionId:" + savedSubmission.getId()));

        SubmissionResponse.SubmissionData data = new SubmissionResponse.SubmissionData(
                savedSubmission.getId(),
                savedSubmission.getGroupId(),
                savedSubmission.getDeliverableType(),
                savedSubmission.getStatus(),
                savedSubmission.getCommitteeId(),
                savedSubmission.getCreatedAt());

        return new SubmissionResponse("success", "Submission successfully created.", data);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionListResponse listSubmissions(Long userId, String role,
                                                   Long teamId, String statusParam,
                                                   Long committeeId, String deliverableTypeParam,
                                                   String deadlineStatus,
                                                   Pageable pageable) {
        // C-13: parse enums from params
        SubmissionStatus statusFilter = parseStatus(statusParam);
        DeliverableType typeFilter = parseDeliverableType(deliverableTypeParam);

        Page<Submission> page;

        if ("COORDINATOR".equalsIgnoreCase(role)) {
            // C-26: honour all filter params
            page = submissionRepository.findWithFilters(teamId, statusFilter, committeeId, typeFilter, pageable);

        } else if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("Student is not assigned to any group."));
            Long groupId = member.getGroup().getId();
            // Students can only see their own group; ignore teamId filter
            page = submissionRepository.findWithFilters(groupId, statusFilter, committeeId, typeFilter, pageable);

        } else if ("PROFESSOR".equalsIgnoreCase(role)) {
            // C-25: professor sees submissions for groups in their assigned committees
            List<Long> committeeIds = committeeAdvisorRepository.findCommitteeIdsByAdvisorUserId(userId);
            if (committeeIds.isEmpty()) {
                page = Page.empty(pageable);
            } else {
                page = submissionRepository.findByCommitteeIdInWithFilters(
                        committeeIds, teamId, statusFilter, typeFilter, pageable);
            }
        } else {
            throw new ForbiddenException("Role '" + role + "' is not authorized to list submissions.");
        }

        Schedule schedule = scheduleRepository.findTopByOrderByIdDesc().orElse(null);

        List<SubmissionSummary> items = page.getContent().stream()
                .map(s -> {
                    String teamName = resolveTeamName(s.getGroupId());
                    LocalDateTime dl = resolveDeadline(s.getDeliverableType(), schedule);
                    Boolean overdue = (dl != null && s.getCreatedAt() != null)
                            ? s.getCreatedAt().isAfter(dl) : null;
                    return new SubmissionSummary(
                            s.getId(),
                            s.getGroupId(),
                            teamName,
                            s.getDeliverableType() != null ? s.getDeliverableType().name() : null,
                            s.getStatus() != null ? s.getStatus().name() : null,
                            s.getCommitteeId(),
                            s.getVersion(),
                            s.getCreatedAt(),
                            dl,
                            overdue);
                })
                .toList();

        // Apply deadline filter on in-memory mapped items (deadline is computed, not stored)
        List<SubmissionSummary> filtered = items;
        if (deadlineStatus != null && !deadlineStatus.isBlank()) {
            if ("OVERDUE".equalsIgnoreCase(deadlineStatus)) {
                filtered = items.stream()
                        .filter(s -> Boolean.TRUE.equals(s.isOverdue()))
                        .toList();
            } else if ("APPROACHING".equalsIgnoreCase(deadlineStatus)) {
                // APPROACHING: deadline exists, not yet passed, submission not overdue
                filtered = items.stream()
                        .filter(s -> s.deadline() != null && !Boolean.TRUE.equals(s.isOverdue()))
                        .toList();
            }
        }

        PaginationMeta meta = new PaginationMeta(
                (int) page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());

        return new SubmissionListResponse("success", filtered, meta);
    }

    @Override
    @Transactional(readOnly = true)
    public SubmissionDetailResponse getSubmission(Long submissionId, Long userId, String role) {
        Submission s = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found: " + submissionId));

        if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("Student is not assigned to any group."));
            if (!member.getGroup().getId().equals(s.getGroupId())) {
                throw new ForbiddenException("You can only view your own group's submissions.");
            }
        } else if (!"PROFESSOR".equalsIgnoreCase(role) && !"COORDINATOR".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Role '" + role + "' is not authorized.");
        }

        String teamName = resolveTeamName(s.getGroupId());

        return new SubmissionDetailResponse(
                s.getId(),
                s.getGroupId(),
                teamName,
                s.getDeliverableType() != null ? s.getDeliverableType().name() : null,
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getContent(),
                s.getFileUrl(),
                s.getCommitteeId(),
                s.getFinalGrade(),
                s.getParentSubmissionId(),
                s.getVersion(),
                s.getCreatedAt());
    }

    @Override
    @Transactional
    public RevisionCreateResponseDto createRevision(Long parentSubmissionId,
                                                    MultipartFile file,
                                                    String description,
                                                    Long callerId) {
        Submission parent = submissionRepository.findById(parentSubmissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + parentSubmissionId));

        if (parent.getStatus() != SubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException(
                    "Cannot submit revision: parent status is " + parent.getStatus()
                    + " but must be REVISION_REQUESTED.");
        }

        Group group = groupRepository.findById(parent.getGroupId())
                .orElseThrow(() -> new NotFoundException("Group not found with ID: " + parent.getGroupId()));

        if (group.getLeader() == null || !group.getLeader().getUserId().equals(callerId)) {
            throw new ForbiddenException("Only the group leader may submit a revision.");
        }

        scheduleRepository.findTopByOrderByIdDesc().ifPresent(schedule -> {
            if (schedule.getProposalRevisionDeadline() != null &&
                    java.time.Instant.now().isAfter(schedule.getProposalRevisionDeadline())) {
                throw new ForbiddenException("Revision deadline has passed (D10).");
            }
        });

        parent.setStatus(SubmissionStatus.SUPERSEDED);
        submissionRepository.save(parent);

        int newVersion = (parent.getVersion() != null ? parent.getVersion() : 1) + 1;

        // C-8: store the actual file
        String fileUrl = fileStorageService.store(file);

        Submission revision = new Submission();
        revision.setGroupId(parent.getGroupId());
        revision.setDeliverableType(parent.getDeliverableType());
        revision.setContent(description != null ? description : "");
        revision.setFileUrl(fileUrl);
        revision.setStatus(SubmissionStatus.PENDING_REVIEW);
        revision.setCommitteeId(parent.getCommitteeId());
        revision.setParentSubmissionId(parentSubmissionId);
        revision.setVersion(newVersion);

        Submission saved = submissionRepository.save(revision);

        GroupCommitteeAssignment assignment = assignmentRepository
                .findTopByGroupIdAndStatusOrderByAssignedAtDesc(parent.getGroupId(), "ASSIGNED")
                .orElseThrow(() -> new ForbiddenException("Group is not assigned to any committee."));

        Committee committee = assignment.getCommittee();
        String notificationMsg = "Revision v" + newVersion + " submitted for submission #"
                + parentSubmissionId + " by Group: " + group.getGroupName();

        if (committee.getAdvisors() != null) {
            committee.getAdvisors().forEach(advisor ->
                    notificationService.createSystemAlert(advisor.getAdvisor().getUserId(), notificationMsg,
                            "REVISION_ALERT", "submissionId:" + saved.getId()));
        }
        if (committee.getJuryMembers() != null) {
            committee.getJuryMembers().forEach(jury ->
                    notificationService.createSystemAlert(jury.getJuryMember().getUserId(), notificationMsg,
                            "REVISION_ALERT", "submissionId:" + saved.getId()));
        }
        userRepository.findAllByRoleIgnoreCase("COORDINATOR").forEach(coord ->
                notificationService.createSystemAlert(coord.getUserId(), notificationMsg,
                        "REVISION_ALERT", "submissionId:" + saved.getId()));

        RevisionCreateResponseDto.Data data = new RevisionCreateResponseDto.Data(
                saved.getId(),
                saved.getParentSubmissionId(),
                saved.getVersion(),
                saved.getStatus().name(),
                saved.getCreatedAt());

        return new RevisionCreateResponseDto("success", "Revision submitted successfully.", data);
    }

    @Override
    @Transactional(readOnly = true)
    public RevisionHistoryResponseDto getRevisionHistory(Long submissionId, Long userId, String role) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found with ID: " + submissionId));

        if ("STUDENT".equalsIgnoreCase(role)) {
            GroupMember member = groupMemberRepository.findTopByUser_UserId(userId)
                    .orElseThrow(() -> new ForbiddenException("Student is not assigned to any group."));
            if (!member.getGroup().getId().equals(submission.getGroupId())) {
                throw new ForbiddenException("You can only view revision history for your own group's submissions.");
            }
        } else if (!"PROFESSOR".equalsIgnoreCase(role) && !"COORDINATOR".equalsIgnoreCase(role)) {
            throw new ForbiddenException("Role '" + role + "' is not authorized to view revision history.");
        }

        // Walk to root
        Submission current = submission;
        while (current.getParentSubmissionId() != null) {
            final Long parentId = current.getParentSubmissionId();
            current = submissionRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalStateException("Broken revision chain: parent not found for ID " + parentId));
        }
        Long absoluteRootId = current.getId();

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
                        s.getContent()))
                .collect(Collectors.toList());

        return new RevisionHistoryResponseDto("success", items);
    }

    private boolean isPartOfChain(Submission s, Long absoluteRootId, List<Submission> pool) {
        if (s.getId().equals(absoluteRootId)) return true;
        Long parentId = s.getParentSubmissionId();
        while (parentId != null) {
            if (parentId.equals(absoluteRootId)) return true;
            final Long current = parentId;
            Submission parent = pool.stream().filter(p -> p.getId().equals(current)).findFirst().orElse(null);
            if (parent == null) break;
            parentId = parent.getParentSubmissionId();
        }
        return false;
    }

    private String resolveTeamName(Long groupId) {
        return groupRepository.findById(groupId)
                .map(Group::getGroupName)
                .orElse("Team #" + groupId);
    }

    private SubmissionStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return SubmissionStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private DeliverableType parseDeliverableType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return DeliverableType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Maps a deliverable type to the relevant Schedule deadline.
     * PROPOSAL / REVISED_PROPOSAL → proposalRevisionDeadline
     * STATEMENT_OF_WORK / DEMONSTRATION → gradingDeadline
     */
    private LocalDateTime resolveDeadline(DeliverableType type, Schedule schedule) {
        if (type == null || schedule == null) return null;
        java.time.Instant instant;
        switch (type) {
            case PROPOSAL:
            case REVISED_PROPOSAL:
                instant = schedule.getProposalRevisionDeadline();
                break;
            case STATEMENT_OF_WORK:
            case DEMONSTRATION:
                instant = schedule.getGradingDeadline();
                break;
            default:
                return null;
        }
        if (instant == null) return null;
        return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }
}

package com.spms.backend.service;

import com.spms.backend.dto.response.FinalGradeResponse;
import com.spms.backend.model.*;
import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.enums.SubmissionStatus;
import com.spms.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalGradeCalculationServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private UserRepository userRepository;
    @Mock private SprintRepository sprintRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private SprintAdvisorGradeRepository sprintAdvisorGradeRepository;
    @Mock private SprintDeliverableWeightRepository sprintDeliverableWeightRepository;
    @Mock private TeamFinalGradeRepository teamFinalGradeRepository;
    @Mock private StudentFinalGradeRepository studentFinalGradeRepository;
    @Mock private SprintIssueTrackingRepository sprintIssueTrackingRepository;
    @Mock private IssueValidationResultRepository issueValidationResultRepository;
    @Mock private NotificationService notificationService;

    private FinalGradeCalculationService service;

    private Group group;
    private User student;
    private Sprint sprint;

    @BeforeEach
    void setUp() {
        service = new FinalGradeCalculationService(
                groupRepository,
                userRepository,
                sprintRepository,
                groupMemberRepository,
                submissionRepository,
                sprintAdvisorGradeRepository,
                sprintDeliverableWeightRepository,
                teamFinalGradeRepository,
                studentFinalGradeRepository,
                sprintIssueTrackingRepository,
                issueValidationResultRepository,
                notificationService);

        student = new User();
        student.setUserId(7L);
        student.setFullName("Student One");
        student.setGithubUsername("student-one");

        group = new Group();
        group.setId(10L);
        group.setGroupName("Team Alpha");
        group.setLeader(student);

        sprint = new Sprint("Sprint 1", LocalDate.now().minusDays(7), LocalDate.now(), "Active");
        sprint.setId(1L);
        sprint.setRequiredStoryPoints(10);
    }

    @Test
    void recalculateGroup_appliesPdfScalarAndIndividualStoryPointRatio() {
        Submission proposal = new Submission();
        proposal.setId(100L);
        proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED);
        proposal.setFinalGrade(80.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L);
        weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL);
        weight.setWeight(new BigDecimal("100.00"));

        User advisor = new User();
        advisor.setUserId(3L);
        SprintAdvisorGrade sprintGrade = new SprintAdvisorGrade();
        sprintGrade.setGroup(group);
        sprintGrade.setSprint(sprint);
        sprintGrade.setAdvisor(advisor);
        sprintGrade.setScrumGrade("A");
        sprintGrade.setCodeReviewGrade("B");

        SprintIssueTracking issue = new SprintIssueTracking(group, sprint, "SPMS-1");
        issue.setAssigneeGithubUsername("student-one");
        issue.setStoryPoints(5);
        issue.setPrMerged(true);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc())
                .thenReturn(List.of(weight));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of(sprintGrade));
        when(teamFinalGradeRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(teamFinalGradeRepository.save(any(TeamFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of());
        when(sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(10L, java.util.Set.of(1L)))
                .thenReturn(List.of(issue));
        when(studentFinalGradeRepository.findByGroup_IdAndUser_UserId(10L, 7L)).thenReturn(Optional.empty());
        when(studentFinalGradeRepository.save(any(StudentFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        FinalGradeResponse response = service.recalculateGroup(10L);

        assertEquals(new BigDecimal("72.00"), response.data().teamGrade());
        assertEquals(new BigDecimal("72.00"), response.data().deliverables().get(0).scaledGrade());
        assertEquals(new BigDecimal("36.00"), response.data().students().get(0).finalGrade());
        assertEquals(new BigDecimal("0.5000"), response.data().students().get(0).spRatio());
    }
}

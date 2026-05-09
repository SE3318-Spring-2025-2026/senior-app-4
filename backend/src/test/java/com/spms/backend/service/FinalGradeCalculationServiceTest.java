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

import com.spms.backend.exception.BadRequestException;

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
        
        org.mockito.Mockito.lenient().when(sprintRepository.findAllByOrderByStartDateAscIdAsc()).thenReturn(List.of(sprint));
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

    @Test
    void missingGradingConfigEdgeCase_throwsBadRequest() {
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc())
                .thenReturn(List.of());

        BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(
                BadRequestException.class,
                () -> service.recalculateGroup(10L)
        );
        assertEquals("Coordinator must configure sprint deliverable weights first.", ex.getMessage());
    }

    @Test
    void softGradingAverage_correctlyComputesWeightedAverage() {
        Submission proposal = new Submission();
        proposal.setId(100L);
        proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED);
        proposal.setFinalGrade(100.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L);
        weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL);
        weight.setWeight(new BigDecimal("100.00"));

        User advisor1 = new User(); advisor1.setUserId(3L);
        User advisor2 = new User(); advisor2.setUserId(4L);
        
        SprintAdvisorGrade grade1 = new SprintAdvisorGrade();
        grade1.setGroup(group); grade1.setSprint(sprint); grade1.setAdvisor(advisor1);
        grade1.setScrumGrade("A"); grade1.setCodeReviewGrade("C");

        SprintAdvisorGrade grade2 = new SprintAdvisorGrade();
        grade2.setGroup(group); grade2.setSprint(sprint); grade2.setAdvisor(advisor2);
        grade2.setScrumGrade("B"); grade2.setCodeReviewGrade("A");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc()).thenReturn(List.of(weight));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of(grade1, grade2));
        
        when(teamFinalGradeRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(teamFinalGradeRepository.save(any(TeamFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of());
        when(sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(10L, java.util.Set.of(1L))).thenReturn(List.of());
        when(studentFinalGradeRepository.findByGroup_IdAndUser_UserId(10L, 7L)).thenReturn(Optional.empty());
        when(studentFinalGradeRepository.save(any(StudentFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        FinalGradeResponse response = service.recalculateGroup(10L);

        assertEquals(new BigDecimal("85.00"), response.data().teamGrade());
        assertEquals(new BigDecimal("90.00"), response.data().deliverables().get(0).scrumAverage());
        assertEquals(new BigDecimal("80.00"), response.data().deliverables().get(0).codeReviewAverage());
    }

    @Test
    void binaryGrading_resolvesAiCodeReviewFallback() {
        Submission proposal = new Submission();
        proposal.setId(100L); proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED); proposal.setFinalGrade(100.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L); weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL); weight.setWeight(new BigDecimal("100.00"));

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc()).thenReturn(List.of(weight));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        
        // No advisor grades
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of());

        BadRequestException ex = org.junit.jupiter.api.Assertions.assertThrows(
                BadRequestException.class,
                () -> service.recalculateGroup(10L)
        );
        assertEquals("Missing Scrum grade for group 10 sprint 1", ex.getMessage());
    }

    @Test
    void storyPointRatio_computesCompletedVsTarget() {
        Submission proposal = new Submission();
        proposal.setId(100L); proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED); proposal.setFinalGrade(100.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L); weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL); weight.setWeight(new BigDecimal("100.00"));

        SprintAdvisorGrade grade1 = new SprintAdvisorGrade();
        grade1.setGroup(group); grade1.setSprint(sprint); grade1.setAdvisor(new User());
        grade1.setScrumGrade("A"); grade1.setCodeReviewGrade("A");

        SprintIssueTracking issue1 = new SprintIssueTracking(group, sprint, "SPMS-1");
        issue1.setAssigneeGithubUsername("student-one");
        issue1.setStoryPoints(8);
        issue1.setPrMerged(true);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc()).thenReturn(List.of(weight));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of(grade1));
        
        when(teamFinalGradeRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(teamFinalGradeRepository.save(any(TeamFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of());
        when(sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(10L, java.util.Set.of(1L))).thenReturn(List.of(issue1));
        when(studentFinalGradeRepository.findByGroup_IdAndUser_UserId(10L, 7L)).thenReturn(Optional.empty());
        when(studentFinalGradeRepository.save(any(StudentFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sprintRepository.findAllByOrderByStartDateAscIdAsc()).thenReturn(List.of(sprint));

        FinalGradeResponse response = service.recalculateGroup(10L);

        assertEquals(new BigDecimal("0.8000"), response.data().students().get(0).spRatio());
        assertEquals(new BigDecimal("80.00"), response.data().students().get(0).finalGrade());
    }

    @Test
    void zeroTargetSPEdgeCase_handlesGracefully() {
        sprint.setRequiredStoryPoints(0);

        Submission proposal = new Submission();
        proposal.setId(100L); proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED); proposal.setFinalGrade(100.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L); weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL); weight.setWeight(new BigDecimal("100.00"));

        SprintAdvisorGrade grade1 = new SprintAdvisorGrade();
        grade1.setGroup(group); grade1.setSprint(sprint); grade1.setAdvisor(new User());
        grade1.setScrumGrade("A"); grade1.setCodeReviewGrade("A");

        SprintIssueTracking issue1 = new SprintIssueTracking(group, sprint, "SPMS-1");
        issue1.setAssigneeGithubUsername("student-one");
        issue1.setStoryPoints(5);
        issue1.setPrMerged(true);

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc()).thenReturn(List.of(weight));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of(grade1));
        
        when(teamFinalGradeRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(teamFinalGradeRepository.save(any(TeamFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of());
        when(sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(10L, java.util.Set.of(1L))).thenReturn(List.of(issue1));
        when(studentFinalGradeRepository.findByGroup_IdAndUser_UserId(10L, 7L)).thenReturn(Optional.empty());
        when(studentFinalGradeRepository.save(any(StudentFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sprintRepository.findAllByOrderByStartDateAscIdAsc()).thenReturn(List.of(sprint));

        FinalGradeResponse response = service.recalculateGroup(10L);

        assertEquals(new BigDecimal("1.0000"), response.data().students().get(0).spRatio());
    }

    @Test
    void deliverableScalar_matchesExampleCalculation() {
        Submission proposal = new Submission();
        proposal.setId(100L); proposal.setGroupId(group.getId());
        proposal.setDeliverableType(DeliverableType.PROPOSAL);
        proposal.setStatus(SubmissionStatus.GRADED); proposal.setFinalGrade(80.0);

        SprintDeliverableWeight weight = new SprintDeliverableWeight();
        weight.setId(1L); weight.setSprint(sprint);
        weight.setDeliverableType(DeliverableType.PROPOSAL); weight.setWeight(new BigDecimal("50.00"));

        SprintDeliverableWeight weight2 = new SprintDeliverableWeight();
        weight2.setId(2L); weight2.setSprint(sprint);
        weight2.setDeliverableType(DeliverableType.STATEMENT_OF_WORK); weight2.setWeight(new BigDecimal("50.00"));

        Submission sow = new Submission();
        sow.setId(101L); sow.setGroupId(group.getId());
        sow.setDeliverableType(DeliverableType.STATEMENT_OF_WORK);
        sow.setStatus(SubmissionStatus.GRADED); sow.setFinalGrade(100.0);

        SprintAdvisorGrade grade1 = new SprintAdvisorGrade();
        grade1.setGroup(group); grade1.setSprint(sprint); grade1.setAdvisor(new User());
        grade1.setScrumGrade("A"); grade1.setCodeReviewGrade("B");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(sprintDeliverableWeightRepository.findAllByOrderByDeliverableTypeAscSprint_IdAsc()).thenReturn(List.of(weight, weight2));
        
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.REVISED_PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.empty());
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.PROPOSAL, SubmissionStatus.GRADED)).thenReturn(Optional.of(proposal));
        when(submissionRepository.findTopByGroupIdAndDeliverableTypeAndStatusOrderByCreatedAtDesc(
                10L, DeliverableType.STATEMENT_OF_WORK, SubmissionStatus.GRADED)).thenReturn(Optional.of(sow));
                
        when(sprintAdvisorGradeRepository.findByGroup_IdAndSprint_Id(10L, 1L)).thenReturn(List.of(grade1));
        
        when(teamFinalGradeRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());
        when(teamFinalGradeRepository.save(any(TeamFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(groupMemberRepository.findByGroup_Id(10L)).thenReturn(List.of());
        when(sprintIssueTrackingRepository.findByGroup_IdAndSprint_IdIn(10L, java.util.Set.of(1L))).thenReturn(List.of());
        when(studentFinalGradeRepository.findByGroup_IdAndUser_UserId(10L, 7L)).thenReturn(Optional.empty());
        when(studentFinalGradeRepository.save(any(StudentFinalGrade.class))).thenAnswer(inv -> inv.getArgument(0));

        FinalGradeResponse response = service.recalculateGroup(10L);

        assertEquals(new BigDecimal("81.00"), response.data().teamGrade());
        assertEquals(new BigDecimal("36.00"), response.data().deliverables().get(0).contribution());
        assertEquals(new BigDecimal("45.00"), response.data().deliverables().get(1).contribution());
    }
}

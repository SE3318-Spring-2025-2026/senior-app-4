/*
 * package com.spms.backend.service;
 * 
 * import com.spms.backend.dto.response.AdvisorDecisionResponseDto;
 * import com.spms.backend.exception.BadRequestException;
 * import com.spms.backend.model.*;
 * import com.spms.backend.model.notification.Notification;
 * import com.spms.backend.model.notification.NotificationStatus;
 * import com.spms.backend.model.notification.NotificationType;
 * import com.spms.backend.repository.*;
 * import com.spms.backend.client.GithubApiClient;
 * import com.spms.backend.client.JiraApiClient;
 * import com.spms.backend.service.impl.GroupServiceImpl;
 * import org.junit.jupiter.api.BeforeEach;
 * import org.junit.jupiter.api.DisplayName;
 * import org.junit.jupiter.api.Test;
 * import org.mockito.ArgumentCaptor;
 * import org.mockito.Mock;
 * import org.mockito.MockitoAnnotations;
 * 
 * import java.util.Collections;
 * import java.util.Optional;
 * 
 * import static org.junit.jupiter.api.Assertions.*;
 * import static org.mockito.ArgumentMatchers.any;
 * import static org.mockito.Mockito.*;
 * 
 * class AdvisorRequestDecisionServiceTest {
 * 
 * private GroupService groupService;
 * 
 * @Mock private GroupRepository groupRepository;
 * 
 * @Mock private GroupMemberRepository groupMemberRepository;
 * 
 * @Mock private UserRepository userRepository;
 * 
 * @Mock private NotificationService notificationService;
 * 
 * @Mock private AuditLogRepository auditLogRepository;
 * 
 * @Mock private StudentAuthorizationService authService;
 * 
 * @Mock private JiraIntegrationRepository jiraIntegrationRepository;
 * 
 * @Mock private GithubIntegrationRepository githubIntegrationRepository;
 * 
 * @Mock private JiraApiClient jiraApiClient;
 * 
 * @Mock private NotificationRepository notificationRepository;
 * 
 * @Mock private GithubApiClient githubApiClient;
 * 
 * private User professor;
 * private User leader;
 * private Group group;
 * private Notification request;
 * 
 * @BeforeEach
 * void setUp() {
 * MockitoAnnotations.openMocks(this);
 * groupService = new GroupServiceImpl(
 * groupRepository, groupMemberRepository, userRepository,
 * notificationService, auditLogRepository, authService,
 * jiraIntegrationRepository, githubIntegrationRepository,
 * jiraApiClient, notificationRepository, githubApiClient
 * );
 * 
 * professor = new User();
 * professor.setUserId(10L);
 * professor.setFullName("Prof. Test");
 * professor.setCurrentAdviseeCount(2);
 * 
 * leader = new User();
 * leader.setUserId(20L);
 * 
 * group = new Group();
 * group.setId(100L);
 * group.setGroupName("Test Group");
 * group.setLeader(leader);
 * 
 * request = new Notification();
 * request.setId(1L);
 * request.setType(NotificationType.ADVISOR_REQUEST);
 * request.setStatus(NotificationStatus.PENDING);
 * request.setToUser(professor);
 * request.setGroupId(100L);
 * }
 * 
 * @Test
 * 
 * @DisplayName("APPROVE sets advisor and increments workload")
 * void approveSetsAdvisorAndIncrementsWorkload() {
 * when(notificationRepository.findById(1L)).thenReturn(Optional.of(request));
 * when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
 * when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
 * when(notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
 * any(), any(), any(), any()))
 * .thenReturn(Collections.emptyList());
 * 
 * AdvisorDecisionResponseDto response =
 * groupService.processAdvisorRequestDecision(10L, 1L, "APPROVE", null);
 * 
 * assertEquals("success", response.status());
 * assertEquals(professor, group.getAdvisor());
 * assertEquals(GroupStatus.ADVISED, group.getStatus());
 * assertEquals(3, professor.getCurrentAdviseeCount());
 * assertEquals(NotificationStatus.ACCEPTED, request.getStatus());
 * 
 * verify(groupRepository).save(group);
 * verify(userRepository).save(professor);
 * verify(notificationRepository).save(request);
 * 
 * // Verify manual AuditLog for APPROVE
 * ArgumentCaptor<AuditLog> auditLogCaptor =
 * ArgumentCaptor.forClass(AuditLog.class);
 * verify(auditLogRepository).save(auditLogCaptor.capture());
 * assertEquals(ActionType.ADVISOR_ASSIGNED,
 * auditLogCaptor.getValue().getActionType());
 * }
 * 
 * @Test
 * 
 * @DisplayName("APPROVE auto-rejects all other PENDING requests from the same group"
 * )
 * void approveAutoRejectsOtherPendingRequests() {
 * Notification otherRequest = new Notification();
 * otherRequest.setId(2L);
 * User otherProf = new User();
 * otherProf.setUserId(11L);
 * otherRequest.setToUser(otherProf);
 * otherRequest.setStatus(NotificationStatus.PENDING);
 * 
 * when(notificationRepository.findById(1L)).thenReturn(Optional.of(request));
 * when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
 * when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
 * when(notificationRepository.findByGroupIdAndTypeAndStatusAndToUser_UserIdNot(
 * 100L, NotificationType.ADVISOR_REQUEST, NotificationStatus.PENDING, 10L))
 * .thenReturn(Collections.singletonList(otherRequest));
 * 
 * groupService.processAdvisorRequestDecision(10L, 1L, "APPROVE", null);
 * 
 * assertEquals(NotificationStatus.REJECTED, otherRequest.getStatus());
 * verify(notificationRepository, atLeastOnce()).save(otherRequest);
 * 
 * // Verify system alert notification for the auto-rejected professor
 * // 1. accepted original request
 * // 2. rejected other request
 * // 3. system alert to other professor
 * // 4. decision notification to student
 * verify(notificationRepository, times(4)).save(any(Notification.class));
 * }
 * 
 * @Test
 * 
 * @DisplayName("REJECT updates status and records reason in audit log")
 * void rejectUpdatesStatusAndRecordsReason() {
 * when(notificationRepository.findById(1L)).thenReturn(Optional.of(request));
 * when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
 * when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
 * 
 * AdvisorDecisionResponseDto response =
 * groupService.processAdvisorRequestDecision(10L, 1L, "REJECT", "No capacity");
 * 
 * assertEquals("success", response.status());
 * assertEquals(NotificationStatus.REJECTED, request.getStatus());
 * assertNull(group.getAdvisor());
 * 
 * ArgumentCaptor<AuditLog> auditLogCaptor =
 * ArgumentCaptor.forClass(AuditLog.class);
 * verify(auditLogRepository).save(auditLogCaptor.capture());
 * assertEquals(ActionType.ADVISOR_REJECTED,
 * auditLogCaptor.getValue().getActionType());
 * assertTrue(auditLogCaptor.getValue().getEventDetails().contains("No capacity"
 * ));
 * }
 * 
 * @Test
 * 
 * @DisplayName("Invalid status throws BadRequestException")
 * void invalidStatusThrowsException() {
 * when(notificationRepository.findById(1L)).thenReturn(Optional.of(request));
 * when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
 * when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
 * 
 * assertThrows(BadRequestException.class, () ->
 * groupService.processAdvisorRequestDecision(10L, 1L, "INVALID", null)
 * );
 * }
 * 
 * @Test
 * 
 * @DisplayName("APPROVE throws ConflictException if group already has an advisor"
 * )
 * void approveThrowsConflictExceptionIfAdvisorAlreadyAssigned() {
 * group.setAdvisor(new User()); // Group already has an advisor
 * when(notificationRepository.findById(1L)).thenReturn(Optional.of(request));
 * when(groupRepository.findById(100L)).thenReturn(Optional.of(group));
 * when(userRepository.findById(10L)).thenReturn(Optional.of(professor));
 * 
 * assertThrows(com.spms.backend.exception.ConflictException.class, () ->
 * groupService.processAdvisorRequestDecision(10L, 1L, "APPROVE", null)
 * );
 * }
 * }
 */
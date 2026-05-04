package com.spms.backend.service;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Group;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.model.JiraIntegrationStatus;
import com.spms.backend.model.User;
import com.spms.backend.repository.AuditLogRepository;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.repository.UserRepository;
import com.spms.backend.repository.NotificationRepository;
import com.spms.backend.service.impl.GroupServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplJiraIntegrationTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupMemberRepository groupMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private StudentAuthorizationService authService;
    @Mock
    private JiraIntegrationRepository jiraIntegrationRepository;
    @Mock
    private GithubIntegrationRepository githubIntegrationRepository;
    @Mock
    private JiraApiClient jiraApiClient;
    @Mock // Ekledik ki hata vermesin
    private GithubApiClient githubApiClient;

    private GroupServiceImpl groupService;
@BeforeEach
    void setUp() {
        groupService = new GroupServiceImpl(
                groupRepository,
                groupMemberRepository,
                userRepository,
                notificationService,
                auditLogRepository,            
                authService,                   
                jiraIntegrationRepository,     
                githubIntegrationRepository,   
                jiraApiClient,                 
                notificationRepository,        
                githubApiClient                
        );
    }

    @Test
    void bindJiraIntegrationSucceedsForLeaderWhenValidationPasses() {
        Group group = groupWithLeader(10L, 100L);
        JiraBindingRequest request = new JiraBindingRequest("https://team.atlassian.net", "test@atlassian.net", "api-key", "ALPHA");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraApiClient.validateSpaceConnection(request.jiraSpaceUrl(), request.projectKey(), request.email(), request.apiKey())).thenReturn(true);

        groupService.bindJiraIntegration(10L, 100L, request);

        verify(jiraIntegrationRepository).save(any(JiraIntegration.class));
        verify(notificationService).createSystemAlert(100L, "JIRA integration is active for group 10", "JIRA_INTEGRATION_STATUS", "https://team.atlassian.net");
    }

    @Test
    void bindJiraIntegrationReturns400WhenValidationFails() {
        Group group = groupWithLeader(10L, 100L);
        JiraBindingRequest request = new JiraBindingRequest("https://team.atlassian.net", "test@atlassian.net", null, "ALPHA");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraApiClient.validateSpaceConnection(request.jiraSpaceUrl(), request.projectKey(), request.email(), request.apiKey())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> groupService.bindJiraIntegration(10L, 100L, request));

        verify(jiraIntegrationRepository, never()).save(any());
    }

    @Test
    void bindJiraIntegrationReturns404WhenGroupDoesNotExist() {
        JiraBindingRequest request = new JiraBindingRequest("https://team.atlassian.net", "test@atlassian.net", null, "ALPHA");
        when(groupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> groupService.bindJiraIntegration(999L, 100L, request));
    }

    @Test
    void getJiraIntegrationReturnsCurrentStatus() {
        Group group = groupWithLeader(10L, 100L);
        JiraIntegration integration = new JiraIntegration();
        integration.setStatus(JiraIntegrationStatus.ACTIVE);
        integration.setJiraSpaceUrl("https://team.atlassian.net");
        integration.setProjectKey("ALPHA");

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(integration));

        JiraIntegrationResponse response = groupService.getJiraIntegration(10L);

        assertEquals("active", response.data().status());
        assertEquals("https://team.atlassian.net", response.data().jiraSpaceUrl());
    }

    @Test
    void getJiraIntegrationReturnsNotConnectedWhenIntegrationMissing() {
        Group group = groupWithLeader(10L, 100L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());

        JiraIntegrationResponse response = groupService.getJiraIntegration(10L);

        assertEquals("inactive", response.data().status());
        assertEquals("Not connected", response.data().message());
    }

    @Test
    void unbindJiraIntegrationDeletesExistingIntegration() {
        Group group = groupWithLeader(10L, 100L);
        JiraIntegration integration = new JiraIntegration();

        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.of(integration));

        groupService.unbindJiraIntegration(10L, 100L);

        verify(jiraIntegrationRepository).delete(integration);
    }

    @Test
    void unbindJiraIntegrationReturns404WhenMissing() {
        Group group = groupWithLeader(10L, 100L);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(jiraIntegrationRepository.findByGroup_Id(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> groupService.unbindJiraIntegration(10L, 100L));
    }

    @Test
    void onlyLeaderCanBindOrUnbindJiraIntegration() {
        Group group = groupWithLeader(10L, 100L);
        JiraBindingRequest request = new JiraBindingRequest("https://team.atlassian.net", "test@atlassian.net", null, "ALPHA");
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));

        assertThrows(ForbiddenException.class, () -> groupService.bindJiraIntegration(10L, 101L, request));
        assertThrows(ForbiddenException.class, () -> groupService.unbindJiraIntegration(10L, 101L));
    }

    private Group groupWithLeader(Long groupId, Long leaderId) {
        User leader = new User();
        leader.setUserId(leaderId);

        Group group = new Group();
        group.setId(groupId);
        group.setLeader(leader);
        return group;
    }
}
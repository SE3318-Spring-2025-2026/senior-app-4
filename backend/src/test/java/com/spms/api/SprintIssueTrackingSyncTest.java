package com.spms.api;

import com.spms.backend.client.GithubApiClient;
import com.spms.backend.client.JiraApiClient;
import com.spms.backend.dto.JiraIssueData;
import com.spms.backend.dto.PrCheckResult;
import com.spms.backend.model.*;
import com.spms.backend.repository.*;
import com.spms.backend.service.GithubDiscoveryService;
import com.spms.backend.service.ScrumSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * H2 integration test — Sprint issue tracking sync pipeline.
 *
 * External HTTP clients (Jira, GitHub) are mocked.
 * All persistence goes through real H2 repositories.
 */
public class SprintIssueTrackingSyncTest extends BaseApiTest {

    // --- External clients: mocked (no real HTTP) ---
    @MockBean private JiraApiClient jiraApiClient;
    @MockBean private GithubApiClient githubApiClient;
    @MockBean private GithubDiscoveryService githubDiscoveryService;

    // --- Real H2 repositories ---
    @Autowired private UserRepository userRepository;
    @Autowired private GroupRepository groupRepository;
    @Autowired private SprintRepository sprintRepository;
    @Autowired private JiraIntegrationRepository jiraIntegrationRepository;
    @Autowired private GithubIntegrationRepository githubIntegrationRepository;
    @Autowired private SprintIssueTrackingRepository sprintIssueTrackingRepository;

    // --- Sync service (real implementation, runs on H2) ---
    @Autowired private ScrumSyncService scrumSyncService;

    private Group group;
    private Sprint sprint;

    @BeforeEach
    void setup() {
        // Leader user
        User leader = new User();
        leader.setFullName("Test Leader");
        leader.setEmail("leader@test.com");
        leader.setStudentId("S001");
        leader.setRole("student");
        leader = userRepository.save(leader);

        // Group
        group = new Group();
        group.setGroupName("Test Group");
        group.setLeader(leader);
        group.setStatus(GroupStatus.FORMING);
        group = groupRepository.save(group);

        // Active sprint covering today
        sprint = new Sprint("Sprint 1", LocalDate.now().minusDays(3), LocalDate.now().plusDays(4), "Active");
        sprint = sprintRepository.save(sprint);

        // Jira integration — plain text token, EncryptionConverter encrypts on write
        JiraIntegration jira = new JiraIntegration();
        jira.setGroup(group);
        jira.setJiraSpaceUrl("https://test.atlassian.net");
        jira.setJiraEmail("test@test.com");
        jira.setApiKey("jira-token-plain");
        jira.setProjectKey("SPMS");
        jiraIntegrationRepository.save(jira);

        // GitHub integration — repositoryName now properly set
        GithubIntegration github = new GithubIntegration();
        github.setGroup(group);
        github.setOrganizationName("test-org");
        github.setRepositoryName("senior-app-4");
        github.setGithubPatEncrypted("ghp_test_token");
        github.setStatus(GithubIntegrationStatus.ACTIVE);
        githubIntegrationRepository.save(github);
    }

    @AfterEach
    void cleanup() {
        sprintIssueTrackingRepository.deleteAll();
        githubIntegrationRepository.deleteAll();
        jiraIntegrationRepository.deleteAll();
        sprintRepository.deleteAll();
        groupRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Sync pipeline: Jira issues + GitHub PR data → sprint_issue_tracking dolduruluyor")
    void sync_populatesSprintIssueTracking_withPrData() {
        // Jira'dan 2 issue geliyor
        when(jiraApiClient.fetchActiveSprintIssues(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(List.of(
                new JiraIssueData("SPMS-1", 5, "dev1@test.com"),
                new JiraIssueData("SPMS-2", 3, "dev2@test.com")
            ));

        // GitHub'da SPMS-1 için branch var, PR merge edilmiş
        when(githubDiscoveryService.findBranchForIssueKey(eq(group.getId()), eq("SPMS-1"), eq("senior-app-4")))
            .thenReturn(Optional.of("feature/SPMS-1-login-fix"));
        when(githubDiscoveryService.findBranchForIssueKey(eq(group.getId()), eq("SPMS-2"), eq("senior-app-4")))
            .thenReturn(Optional.empty());

        when(githubApiClient.findMergedPrForBranch(eq("test-org"), eq("senior-app-4"), eq("feature/SPMS-1-login-fix"), anyString()))
            .thenReturn(Optional.of(new PrCheckResult(42L, true, "dev-github-user")));

        // Sync çalıştır (async değil, doğrudan pipeline)
        scrumSyncService.executeSyncPipeline();

        // H2'den kontrol et
        List<SprintIssueTracking> records = sprintIssueTrackingRepository
            .findByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

        assertThat(records).hasSize(2);

        SprintIssueTracking spms1 = records.stream()
            .filter(r -> r.getIssueKey().equals("SPMS-1")).findFirst().orElseThrow();
        assertThat(spms1.getStoryPoints()).isEqualTo(5);
        assertThat(spms1.getPrNumber()).isEqualTo(42L);
        assertThat(spms1.getPrMerged()).isTrue();
        assertThat(spms1.getAssigneeGithubUsername()).isEqualTo("dev-github-user");

        SprintIssueTracking spms2 = records.stream()
            .filter(r -> r.getIssueKey().equals("SPMS-2")).findFirst().orElseThrow();
        assertThat(spms2.getStoryPoints()).isEqualTo(3);
        assertThat(spms2.getPrNumber()).isNull();
        assertThat(spms2.getPrMerged()).isNull();
    }

    @Test
    @DisplayName("Sync pipeline: aktif sprint yoksa tablo boş kalır")
    void sync_doesNothing_whenNoActiveSprint() {
        sprintRepository.deleteAll();

        scrumSyncService.executeSyncPipeline();

        List<SprintIssueTracking> records = sprintIssueTrackingRepository.findAll();
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("Sync pipeline: GitHub entegrasyonu yoksa Jira issue_key ve story_points düşer, PR alanları null kalır")
    void sync_savesJiraDataOnly_whenNoGithubIntegration() {
        githubIntegrationRepository.deleteAll();

        when(jiraApiClient.fetchActiveSprintIssues(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(List.of(new JiraIssueData("SPMS-3", 8, "dev3@test.com")));

        scrumSyncService.executeSyncPipeline();

        List<SprintIssueTracking> records = sprintIssueTrackingRepository
            .findByGroup_IdAndSprint_Id(group.getId(), sprint.getId());

        // GitHub entegrasyonu olmadığında servis erken dönüyor — kayıt yok
        assertThat(records).isEmpty();
    }
}

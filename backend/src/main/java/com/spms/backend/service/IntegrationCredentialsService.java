package com.spms.backend.service;

import com.spms.backend.dto.response.CredentialsResponse;
import com.spms.backend.model.Group;
import com.spms.backend.model.GithubIntegration;
import com.spms.backend.model.JiraIntegration;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.GithubIntegrationRepository;
import com.spms.backend.repository.JiraIntegrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IntegrationCredentialsService {

    private final GroupRepository groupRepository;
    private final JiraIntegrationRepository jiraIntegrationRepository;
    private final GithubIntegrationRepository githubIntegrationRepository;

    public IntegrationCredentialsService(
            GroupRepository groupRepository,
            JiraIntegrationRepository jiraIntegrationRepository,
            GithubIntegrationRepository githubIntegrationRepository) {
        this.groupRepository = groupRepository;
        this.jiraIntegrationRepository = jiraIntegrationRepository;
        this.githubIntegrationRepository = githubIntegrationRepository;
    }

    @Transactional(readOnly = true)
    public List<CredentialsResponse> getCredentialsByTeamId(Long teamId) {
        Group group = groupRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team (Group) not found: " + teamId));

        List<CredentialsResponse> credentials = new ArrayList<>();

        jiraIntegrationRepository.findByGroup_Id(teamId).ifPresent(jiraIntegration -> {
            if (jiraIntegration.getApiKey() != null) {
                credentials.add(new CredentialsResponse(
                        "JIRA",
                        jiraIntegration.getApiKey(),
                        jiraIntegration.getProjectKey(),
                        null
                ));
            }
        });

        githubIntegrationRepository.findByGroup_Id(teamId).ifPresent(githubIntegration -> {
            if (githubIntegration.getGithubPatEncrypted() != null) {
                credentials.add(new CredentialsResponse(
                        "GITHUB",
                        githubIntegration.getGithubPatEncrypted(),
                        null,
                        githubIntegration.getOrganizationName()
                ));
            }
        });

        return credentials;
    }
}

package com.spms.backend.service;

import com.spms.backend.dto.response.GroupSprintSummaryResponse;
import com.spms.backend.dto.response.GroupTrackingDetailResponse;
import com.spms.backend.dto.response.PerStudentSummaryDto;
import com.spms.backend.model.Group;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdvisorSprintService {

    private final GroupRepository groupRepository;
    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;

    public AdvisorSprintService(GroupRepository groupRepository, SprintIssueTrackingRepository sprintIssueTrackingRepository) {
        this.groupRepository = groupRepository;
        this.sprintIssueTrackingRepository = sprintIssueTrackingRepository;
    }

    public GroupSprintSummaryResponse buildSprintSummary(List<Group> advisorGroups) {
        List<GroupSprintSummaryResponse.GroupSummaryDto> groupSummaries = new ArrayList<>();

        for (Group group : advisorGroups) {
            List<SprintIssueTracking> logs = sprintIssueTrackingRepository.findByGroup_IdAndSprint_Id(group.getId(), 1L);
            if (logs.isEmpty()) continue;

            int totalIssues = logs.size();
            int mergedPRCount = (int) logs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getPrMerged()))
                .count();

            Map<String, PerStudentSummaryDto> studentMap = new HashMap<>();
            for (SprintIssueTracking log : logs) {
                if (log.getAssigneeGithubUsername() != null) {
                    PerStudentSummaryDto student = studentMap.computeIfAbsent(
                        log.getAssigneeGithubUsername(),
                        k -> new PerStudentSummaryDto(k, 0, 0)
                    );

                    int sp = log.getStoryPoints() != null ? log.getStoryPoints() : 0;
                    student.setTotalAssignedStoryPoints(student.getTotalAssignedStoryPoints() + sp);

                    if (Boolean.TRUE.equals(log.getPrMerged())) {
                        student.setCompletedStoryPoints(student.getCompletedStoryPoints() + sp);
                    }
                }
            }

            GroupSprintSummaryResponse.GroupSummaryDto summary = new GroupSprintSummaryResponse.GroupSummaryDto();
            summary.setGroupId(group.getId());
            summary.setGroupName(group.getGroupName());
            summary.setTotalIssues(totalIssues);
            summary.setMergedPRCount(mergedPRCount);
            summary.setSyncedAt(logs.get(0).getSyncedAt());
            summary.setPerStudentSummary(new ArrayList<>(studentMap.values()));

            groupSummaries.add(summary);
        }

        GroupSprintSummaryResponse.ActiveSprintInfo activeSprint = new GroupSprintSummaryResponse.ActiveSprintInfo();
        activeSprint.setSprintId(1L);
        activeSprint.setSprintName("Active Sprint");
        activeSprint.setStartDate(LocalDate.now());
        activeSprint.setEndDate(LocalDate.now().plusDays(14));
        activeSprint.setDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.now().plusDays(14)));

        GroupSprintSummaryResponse response = new GroupSprintSummaryResponse();
        response.setActiveSprint(activeSprint);
        response.setGroups(groupSummaries);

        return response;
    }

    public GroupTrackingDetailResponse buildGroupTrackingDetail(Group group) {
        List<SprintIssueTracking> logs = sprintIssueTrackingRepository.findByGroup_IdAndSprint_Id(group.getId(), 1L);

        List<GroupTrackingDetailResponse.IssueTrackingDto> issueDtos = logs.stream()
            .map(log -> new GroupTrackingDetailResponse.IssueTrackingDto(
                log.getIssueKey(),
                log.getStoryPoints(),
                log.getAssigneeGithubUsername(),
                log.getPrNumber(),
                log.getPrMerged()
            ))
            .toList();

        GroupTrackingDetailResponse response = new GroupTrackingDetailResponse();
        response.setGroupId(group.getId());
        response.setGroupName(group.getGroupName());
        response.setSprintId(1L);
        response.setSyncedAt(logs.isEmpty() ? null : logs.get(0).getSyncedAt());
        response.setIssues(issueDtos);

        return response;
    }
}

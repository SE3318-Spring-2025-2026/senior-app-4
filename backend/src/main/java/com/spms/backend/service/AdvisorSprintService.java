package com.spms.backend.service;

import com.spms.backend.dto.response.GroupSprintSummaryResponse;
import com.spms.backend.dto.response.GroupTrackingDetailResponse;
import com.spms.backend.dto.response.PerStudentSummaryDto;
import com.spms.backend.model.Group;
import com.spms.backend.model.Sprint;
import com.spms.backend.model.SprintIssueTracking;
import com.spms.backend.repository.SprintIssueTrackingRepository;
import com.spms.backend.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvisorSprintService {

    private final SprintIssueTrackingRepository sprintIssueTrackingRepository;
    private final SprintRepository sprintRepository;

    public GroupSprintSummaryResponse buildSprintSummary(List<Group> advisorGroups) {
        Optional<Sprint> activeSprintOpt = sprintRepository.findActiveSprintByDate(LocalDate.now());

        GroupSprintSummaryResponse.ActiveSprintInfo activeSprintInfo = new GroupSprintSummaryResponse.ActiveSprintInfo();
        if (activeSprintOpt.isPresent()) {
            Sprint sprint = activeSprintOpt.get();
            activeSprintInfo.setSprintId(sprint.getId());
            activeSprintInfo.setSprintName(sprint.getSprintName());
            activeSprintInfo.setStartDate(sprint.getStartDate());
            activeSprintInfo.setEndDate(sprint.getEndDate());
            activeSprintInfo.setDaysRemaining(ChronoUnit.DAYS.between(LocalDate.now(), sprint.getEndDate()));
            activeSprintInfo.setRequiredStoryPoints(sprint.getRequiredStoryPoints());
        } else {
            activeSprintInfo.setSprintId(null);
            activeSprintInfo.setSprintName("No active sprint");
            activeSprintInfo.setStartDate(LocalDate.now());
            activeSprintInfo.setEndDate(LocalDate.now());
            activeSprintInfo.setDaysRemaining(0L);
            activeSprintInfo.setRequiredStoryPoints(null);
        }

        List<GroupSprintSummaryResponse.GroupSummaryDto> groupSummaries = new ArrayList<>();

        if (activeSprintOpt.isPresent()) {
            Long sprintId = activeSprintOpt.get().getId();

            for (Group group : advisorGroups) {
                List<SprintIssueTracking> logs = sprintIssueTrackingRepository.findByGroup_IdAndSprint_Id(group.getId(), sprintId);
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
        }

        GroupSprintSummaryResponse response = new GroupSprintSummaryResponse();
        response.setActiveSprint(activeSprintInfo);
        response.setGroups(groupSummaries);

        return response;
    }

    public GroupTrackingDetailResponse buildGroupTrackingDetail(Group group) {
        Optional<Sprint> activeSprintOpt = sprintRepository.findActiveSprintByDate(LocalDate.now());
        Long sprintId = activeSprintOpt.map(Sprint::getId).orElse(null);

        List<SprintIssueTracking> logs = sprintId != null
            ? sprintIssueTrackingRepository.findByGroup_IdAndSprint_Id(group.getId(), sprintId)
            : List.of();

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
        response.setSprintId(sprintId);
        response.setSyncedAt(logs.isEmpty() ? null : logs.get(0).getSyncedAt());
        response.setIssues(issueDtos);

        return response;
    }
}

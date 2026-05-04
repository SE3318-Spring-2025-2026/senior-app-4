package com.spms.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSprintSummaryResponse {

    private ActiveSprintInfo activeSprint;
    private List<GroupSummaryDto> groups;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveSprintInfo {
        private Long sprintId;
        private String sprintName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Long daysRemaining;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupSummaryDto {
        private Long groupId;
        private String groupName;
        private Integer totalIssues;
        private Integer mergedPRCount;
        private Instant syncedAt;
        private List<PerStudentSummaryDto> perStudentSummary;
    }
}

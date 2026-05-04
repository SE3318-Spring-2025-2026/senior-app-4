package com.spms.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupTrackingDetailResponse {
    private Long groupId;
    private String groupName;
    private Long sprintId;
    private Instant syncedAt;
    private List<IssueTrackingDto> issues;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueTrackingDto {
        private String issueKey;
        private Integer storyPoints;
        private String assigneeGithubUsername;
        private Long prNumber;
        private Boolean prMerged;
    }
}

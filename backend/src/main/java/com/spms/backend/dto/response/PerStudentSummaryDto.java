package com.spms.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerStudentSummaryDto {
    private String githubUsername;
    private Integer completedStoryPoints;
    private Integer totalAssignedStoryPoints;
}

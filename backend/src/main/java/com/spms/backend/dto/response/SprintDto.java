package com.spms.backend.dto.response;

import java.time.LocalDate;

public class SprintDto {

    private Long id;
    private String sprintName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public SprintDto(Long id, String sprintName, LocalDate startDate, LocalDate endDate, String status) {
        this.id = id;
        this.sprintName = sprintName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getSprintName() { return sprintName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getStatus() { return status; }
}

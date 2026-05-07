package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class SprintUpdateRequestDto {

    @NotBlank
    private String sprintName;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    public String getSprintName() { return sprintName; }
    public void setSprintName(String sprintName) { this.sprintName = sprintName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}

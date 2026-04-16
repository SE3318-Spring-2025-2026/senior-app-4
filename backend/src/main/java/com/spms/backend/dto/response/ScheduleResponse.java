package com.spms.backend.dto.response;

public class ScheduleResponse {

    private Long id;
    private String groupFormationDeadline;
    private String advisorAssignmentDeadline;
    private String updatedAt;

    public ScheduleResponse() {}

    public ScheduleResponse(Long id,
                            String groupFormationDeadline,
                            String advisorAssignmentDeadline,
                            String updatedAt) {
        this.id = id;
        this.groupFormationDeadline = groupFormationDeadline;
        this.advisorAssignmentDeadline = advisorAssignmentDeadline;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGroupFormationDeadline() { return groupFormationDeadline; }
    public void setGroupFormationDeadline(String groupFormationDeadline) {
        this.groupFormationDeadline = groupFormationDeadline;
    }

    public String getAdvisorAssignmentDeadline() { return advisorAssignmentDeadline; }
    public void setAdvisorAssignmentDeadline(String advisorAssignmentDeadline) {
        this.advisorAssignmentDeadline = advisorAssignmentDeadline;
    }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}

package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class ScheduleUpdateRequest {

    @NotNull(message = "groupFormationDeadline is required")
    private Instant groupFormationDeadline;

    @NotNull(message = "advisorAssignmentDeadline is required")
    private Instant advisorAssignmentDeadline;

    public ScheduleUpdateRequest() {}

    public Instant getGroupFormationDeadline() { return groupFormationDeadline; }
    public void setGroupFormationDeadline(Instant groupFormationDeadline) {
        this.groupFormationDeadline = groupFormationDeadline;
    }

    public Instant getAdvisorAssignmentDeadline() { return advisorAssignmentDeadline; }
    public void setAdvisorAssignmentDeadline(Instant advisorAssignmentDeadline) {
        this.advisorAssignmentDeadline = advisorAssignmentDeadline;
    }
}

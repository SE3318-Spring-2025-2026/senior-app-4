package com.spms.backend.dto.response;

import java.util.List;

public record GroupFormationReportDto(
        long totalGroups,
        long formedGroups,
        long unadvisedGroups,
        List<GroupStatusDetail> details
) {
    public record GroupStatusDetail(
            Long groupId,
            String groupName,
            String status,
            Long advisorId,
            String advisorName
    ) {}
}

package com.spms.backend.dto.response;

import java.util.List;

public record ValidationRulesResponse(
        int minAdvisors,
        int maxAdvisors,
        int minJury,
        int maxJury,
        String scheduleWindow,
        List<String> explicitRules
) {
}

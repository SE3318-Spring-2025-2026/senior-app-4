package com.spms.backend.dto.response;

public record ResetLinkResponse(
        String message,
        String resetToken,
        String resetUrl
) {}

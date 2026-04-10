package com.spms.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResetLinkRequest(
        @NotBlank(message = "targetEmail is required.")
        @Email(message = "targetEmail must be a valid email address.")
        String targetEmail
) {}

package com.spms.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * P5.7 — POST /api/v1/committees/{committeeId}/notify request body.
 * recipients is optional: if null/empty, all committee members are notified.
 */
public record CommitteeNotifyRequestDto(
        @NotBlank(message = "Message must not be blank")
        @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
        String message,

        @NotBlank(message = "notificationType must not be blank")
        String notificationType,

        List<Long> recipients
) {}

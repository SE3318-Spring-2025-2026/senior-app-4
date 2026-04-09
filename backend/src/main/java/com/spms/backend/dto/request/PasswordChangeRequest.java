package com.spms.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(

        @Email(message = "Email must be valid.")
        @NotBlank(message = "Email is required.")
        String email,

        @NotBlank(message = "Temporary password is required.")
        String tempPassword,

        @NotBlank(message = "New password is required.")
        String newPassword

) {
}
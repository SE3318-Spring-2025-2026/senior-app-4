package com.spms.backend.dto.request;

public record PasswordChangeRequest(

        // Optional — required only when no JWT token is provided
        String email,

        // Optional — required only when no JWT token is provided
        String tempPassword,

        String newPassword

) {
}
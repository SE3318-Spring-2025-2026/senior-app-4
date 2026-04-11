package com.spms.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfessorRegisterRequest(
        @NotBlank(message = "fullName is required.")
        String fullName,
        @NotBlank(message = "email is required.")
        @Email(message = "email must be a valid email address.")
        String email,
        @NotBlank(message = "password is required.")
        String password,
        @NotBlank(message = "role is required.")
        String role
) {
}

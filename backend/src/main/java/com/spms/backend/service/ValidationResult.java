package com.spms.backend.service;

public record ValidationResult(boolean valid, String reason) {

    public static ValidationResult success(String reason) {
        return new ValidationResult(true, reason);
    }

    public static ValidationResult failure(String reason) {
        return new ValidationResult(false, reason);
    }
}

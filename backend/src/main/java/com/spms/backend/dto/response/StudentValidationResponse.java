package com.spms.backend.dto.response;

public class StudentValidationResponse {

    private boolean valid;

    public StudentValidationResponse(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() {
        return valid;
    }
}
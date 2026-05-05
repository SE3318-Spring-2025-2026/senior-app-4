package com.spms.backend.exception;

import org.springframework.http.HttpStatus;

public class P7ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public P7ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

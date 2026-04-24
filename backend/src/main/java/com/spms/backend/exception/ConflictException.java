package com.spms.backend.exception;

/**
 * Thrown when a request conflicts with the current state of a resource.
 * Maps to HTTP 409 Conflict.
 *
 * Example: [P4-CONFLICT-1] — Group already has an active advisor.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

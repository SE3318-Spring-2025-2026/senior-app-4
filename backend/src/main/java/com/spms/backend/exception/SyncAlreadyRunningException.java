package com.spms.backend.exception;

/**
 * Thrown when a sync operation is already in progress and another is attempted.
 */
public class SyncAlreadyRunningException extends RuntimeException {

    public SyncAlreadyRunningException(String message) {
        super(message);
    }

    public SyncAlreadyRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}

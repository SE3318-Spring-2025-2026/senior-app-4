package com.spms.backend.exception;

public class GithubAuthenticationException extends RuntimeException {

    public GithubAuthenticationException(String message) {
        super(message);
    }

    public GithubAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

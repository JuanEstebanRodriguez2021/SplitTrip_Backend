package com.splittrip.exception;

import org.springframework.http.HttpStatus;

/** Business error with an explicit HTTP status (409 duplicate email, 401 bad login, etc.). */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
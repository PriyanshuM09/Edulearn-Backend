package com.edulearn.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all API Gateway specific exceptions.
 */
public class GatewayException extends RuntimeException {

    private final HttpStatus status;

    public GatewayException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

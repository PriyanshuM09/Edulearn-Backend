package com.edulearn.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is missing the Authorization header entirely.
 */
public class UnauthorizedException extends GatewayException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

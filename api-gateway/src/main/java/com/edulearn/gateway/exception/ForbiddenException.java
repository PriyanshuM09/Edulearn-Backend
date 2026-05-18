package com.edulearn.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user does not have the required role to access a resource.
 */
public class ForbiddenException extends GatewayException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}

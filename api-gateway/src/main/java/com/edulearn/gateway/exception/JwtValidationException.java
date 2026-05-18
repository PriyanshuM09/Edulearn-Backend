package com.edulearn.gateway.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a JWT token is missing, malformed, or expired.
 */
public class JwtValidationException extends GatewayException {

    public JwtValidationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}

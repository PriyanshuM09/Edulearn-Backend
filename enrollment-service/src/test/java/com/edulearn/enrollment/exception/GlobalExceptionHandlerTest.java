package com.edulearn.enrollment.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleEnrollmentNotFound_ReturnsNotFoundResponse() {
        when(request.getRequestURI()).thenReturn("/api/v1/enrollments/99");

        ResponseEntity<ErrorResponse> response = handler.handleEnrollmentNotFound(
                new EnrollmentNotFoundException("missing"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("/api/v1/enrollments/99", response.getBody().getPath());
    }

    @Test
    void handleAlreadyEnrolled_ReturnsConflictResponse() {
        when(request.getRequestURI()).thenReturn("/api/v1/enrollments");

        ResponseEntity<ErrorResponse> response = handler.handleAlreadyEnrolled(
                new AlreadyEnrolledException("duplicate"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("duplicate", response.getBody().getMessage());
    }

    @Test
    void handleIllegalState_ReturnsBadRequestResponse() {
        when(request.getRequestURI()).thenReturn("/certificate");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalState(
                new IllegalStateException("not complete"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getError());
    }

    @Test
    void handleIllegalArgument_ReturnsBadRequestResponse() {
        when(request.getRequestURI()).thenReturn("/progress");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(
                new IllegalArgumentException("bad progress"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad progress", response.getBody().getMessage());
    }

    @Test
    void handleValidationErrors_ReturnsFieldMessages() {
        EnrollmentRequest target = new EnrollmentRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "request");
        bindingResult.addError(new FieldError("request", "studentId", "Student ID is required"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Student ID is required", response.getBody().get("studentId"));
    }

    @Test
    void handleGeneral_ReturnsInternalServerErrorResponse() {
        when(request.getRequestURI()).thenReturn("/api/v1/enrollments");

        ResponseEntity<ErrorResponse> response = handler.handleGeneral(
                new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
        assertNotNull(response.getBody().getTimestamp());
    }

    private static class EnrollmentRequest {
        @SuppressWarnings("unused")
        private Integer studentId;
    }
}

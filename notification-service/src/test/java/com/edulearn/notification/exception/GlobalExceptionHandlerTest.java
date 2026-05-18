package com.edulearn.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void handleNotFound_ReturnsNotFound() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications/99");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new NotificationNotFoundException(99), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("/api/v1/notifications/99", response.getBody().getPath());
    }

    @Test
    void handleValidation_ReturnsJoinedFieldMessages() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Request(), "request");
        bindingResult.addError(new FieldError("request", "title", "Title is required"));
        bindingResult.addError(new FieldError("request", "message", "Message is required"));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new MethodArgumentNotValidException(null, bindingResult), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Title is required, Message is required", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_ReturnsInternalServerError() {
        when(request.getRequestURI()).thenReturn("/api/v1/notifications");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Something went wrong.", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    private static class Request {
        @SuppressWarnings("unused")
        private String title;
    }
}

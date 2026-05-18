package com.edulearn.payment.exception;

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
    void handlePaymentNotFound_ReturnsNotFound() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments/99");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentNotFound(
                new PaymentNotFoundException(99), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Not Found", response.getBody().getError());
    }

    @Test
    void handleSubscriptionNotFound_ReturnsNotFound() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments/subscriptions/99");

        ResponseEntity<ErrorResponse> response = handler.handleSubscriptionNotFound(
                new SubscriptionNotFoundException(99), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
    }

    @Test
    void handleDuplicatePayment_ReturnsConflict() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments/create-order");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicatePayment(
                new DuplicatePaymentException(1, 101), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Conflict", response.getBody().getError());
    }

    @Test
    void handleValidation_ReturnsBadRequest() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments");
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Request(), "request");
        bindingResult.addError(new FieldError("request", "amount", "Amount is required"));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new MethodArgumentNotValidException(null, bindingResult), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Amount is required", response.getBody().getMessage());
    }

    @Test
    void handleGeneric_ReturnsInternalServerError() {
        when(request.getRequestURI()).thenReturn("/api/v1/payments");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody().getTimestamp());
    }

    private static class Request {
        @SuppressWarnings("unused")
        private Double amount;
    }
}

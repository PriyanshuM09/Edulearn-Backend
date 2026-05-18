package com.edulearn.payment.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Integer paymentId) {
        super("Payment not found with id: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
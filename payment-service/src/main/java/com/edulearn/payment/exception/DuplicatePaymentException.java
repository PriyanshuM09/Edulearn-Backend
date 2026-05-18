package com.edulearn.payment.exception;

public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(Integer studentId, Integer courseId) {
        super("Student " + studentId +
              " has already purchased course " + courseId);
    }
}
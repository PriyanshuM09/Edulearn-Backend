package com.edulearn.payment.exception;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(Integer subscriptionId) {
        super("Subscription not found with id: " + subscriptionId);
    }

    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
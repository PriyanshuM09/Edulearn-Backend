package com.edulearn.discussion.exception;

public class ThreadNotFoundException extends RuntimeException {

    public ThreadNotFoundException(Integer threadId) {
        super("Thread not found with id: " + threadId);
    }

    public ThreadNotFoundException(String message) {
        super(message);
    }
}
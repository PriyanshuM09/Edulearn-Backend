package com.edulearn.progress.exception;

public class ProgressNotFoundException extends RuntimeException {

    public ProgressNotFoundException(Integer progressId) {
        super("Progress not found with id: " + progressId);
    }

    public ProgressNotFoundException(String message) {
        super(message);
    }
}
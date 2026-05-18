package com.edulearn.lesson.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Integer resourceId) {
        super("Resource not found with id: " + resourceId);
    }
}
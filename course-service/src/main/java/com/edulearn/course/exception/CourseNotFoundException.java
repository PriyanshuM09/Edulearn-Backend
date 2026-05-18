package com.edulearn.course.exception;

public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Integer courseId) {
        super("Course not found with id: " + courseId);
    }

    public CourseNotFoundException(String message) {
        super(message);
    }
}
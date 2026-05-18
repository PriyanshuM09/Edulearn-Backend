package com.edulearn.lesson.exception;

public class LessonNotFoundException extends RuntimeException {

    public LessonNotFoundException(Integer lessonId) {
        super("Lesson not found with id: " + lessonId);
    }
}
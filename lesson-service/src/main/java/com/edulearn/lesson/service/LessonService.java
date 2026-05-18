package com.edulearn.lesson.service;

import com.edulearn.lesson.dto.*;

import java.util.List;

public interface LessonService {

    LessonResponseDto addLesson(LessonRequestDto requestDto);

    List<LessonResponseDto> getLessonsByCourse(Integer courseId);

    LessonResponseDto getLessonById(Integer lessonId);

    LessonResponseDto updateLesson(Integer lessonId, LessonRequestDto requestDto);

    void deleteLesson(Integer lessonId);

    void reorderLessons(Integer courseId, List<Integer> orderedLessonIds);

    ResourceResponseDto addResource(Integer lessonId, ResourceRequestDto requestDto);

    void removeResource(Integer lessonId, Integer resourceId);

    List<LessonResponseDto> getPreviewLessons(Integer courseId);
}
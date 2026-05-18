package com.edulearn.lesson.repository;

import com.edulearn.lesson.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {

    List<Lesson> findByCourseId(Integer courseId);

    Optional<Lesson> findByLessonId(Integer lessonId);

    List<Lesson> findByCourseIdOrderByOrderIndex(Integer courseId);

    List<Lesson> findByContentType(String contentType);

    List<Lesson> findByIsPreview(Boolean isPreview);

    long countByCourseId(Integer courseId);
}
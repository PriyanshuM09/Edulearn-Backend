package com.edulearn.progress.repository;

import com.edulearn.progress.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, Integer> {
    Optional<Progress> findByStudentIdAndLessonId(Integer studentId, Integer lessonId);
    List<Progress> findByStudentIdAndCourseId(Integer studentId, Integer courseId);
    List<Progress> findByStudentId(Integer studentId);
}
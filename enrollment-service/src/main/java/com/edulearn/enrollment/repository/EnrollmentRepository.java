package com.edulearn.enrollment.repository;

import com.edulearn.enrollment.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    List<Enrollment> findByStudentId(Integer studentId);

    List<Enrollment> findByCourseId(Integer courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(Integer studentId, Integer courseId);

    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);

    List<Enrollment> findByStatus(String status);

    long countByCourseId(Integer courseId);

    List<Enrollment> findByStudentIdAndStatus(Integer studentId, String status);
}
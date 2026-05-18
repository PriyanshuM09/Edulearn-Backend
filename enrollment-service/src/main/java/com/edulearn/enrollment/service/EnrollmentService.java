package com.edulearn.enrollment.service;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;

import java.util.List;

public interface EnrollmentService {

    EnrollmentResponseDto enroll(EnrollmentRequestDto request);

    void unenroll(Integer enrollmentId);

    List<EnrollmentResponseDto> getEnrollmentsByStudent(Integer studentId);

    List<EnrollmentResponseDto> getEnrollmentsByCourse(Integer courseId);

    EnrollmentResponseDto getEnrollmentById(Integer enrollmentId);

    EnrollmentResponseDto updateProgress(Integer enrollmentId, Integer progressPercent);

    EnrollmentResponseDto markComplete(Integer enrollmentId);

    boolean isEnrolled(Integer studentId, Integer courseId);

    EnrollmentResponseDto getEnrollmentByStudentAndCourse(Integer studentId, Integer courseId);

    EnrollmentResponseDto issueCertificate(Integer enrollmentId);

    long getEnrollmentCount(Integer courseId);

    void cancelEnrollment(Integer enrollmentId);
 // Add at bottom of interface
    List<EnrollmentResponseDto> getAllEnrollments();
}
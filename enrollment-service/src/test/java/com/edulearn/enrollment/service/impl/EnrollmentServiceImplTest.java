package com.edulearn.enrollment.service.impl;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.entity.Enrollment;
import com.edulearn.enrollment.exception.AlreadyEnrolledException;
import com.edulearn.enrollment.mapper.EnrollmentMapper;
import com.edulearn.enrollment.messaging.RabbitMQProducer;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @Mock
    private RabbitMQProducer rabbitMQProducer;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    @Test
    @DisplayName("Test Enroll Student - Success")
    void enroll_Success() {
        // given
        EnrollmentRequestDto request = new EnrollmentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);

        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(1);
        EnrollmentResponseDto response = new EnrollmentResponseDto();
        response.setEnrollmentId(1);

        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
        when(enrollmentMapper.toDto(any(Enrollment.class))).thenReturn(response);

        // when
        EnrollmentResponseDto result = enrollmentService.enroll(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.getEnrollmentId());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Test Enroll Student - Already Enrolled")
    void enroll_AlreadyEnrolled() {
        // given
        EnrollmentRequestDto request = new EnrollmentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);

        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(true);

        // when & then
        assertThrows(AlreadyEnrolledException.class, () -> enrollmentService.enroll(request));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    @DisplayName("Test Get Enrollments By Student - Success")
    void getEnrollmentsByStudent_Success() {
        // given
        Integer studentId = 1;
        when(enrollmentRepository.findByStudentId(studentId)).thenReturn(List.of(new Enrollment()));
        when(enrollmentMapper.toDto(any(Enrollment.class))).thenReturn(new EnrollmentResponseDto());

        // when
        List<EnrollmentResponseDto> result = enrollmentService.getEnrollmentsByStudent(studentId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Enroll Student - Event Publish Failure Still Returns Enrollment")
    void enroll_EventPublishFailure_ReturnsEnrollment() {
        EnrollmentRequestDto request = new EnrollmentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);

        Enrollment saved = enrollment(10, 1, 101, "ACTIVE", 0, false);
        EnrollmentResponseDto response = response(10, 1, 101, "ACTIVE", 0, false);

        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);
        doThrow(new RuntimeException("broker down"))
                .when(rabbitMQProducer).sendEnrollmentCreatedEvent(any());
        when(enrollmentMapper.toDto(saved)).thenReturn(response);

        EnrollmentResponseDto result = enrollmentService.enroll(request);

        assertEquals(10, result.getEnrollmentId());
        verify(rabbitMQProducer).sendEnrollmentCreatedEvent(any());
    }

    @Test
    @DisplayName("Test Unenroll - Deletes Enrollment And Publishes Cancellation")
    void unenroll_DeletesAndPublishesCancellation() {
        Enrollment enrollment = enrollment(10, 1, 101, "ACTIVE", 25, false);
        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));

        enrollmentService.unenroll(10);

        verify(enrollmentRepository).delete(enrollment);
        verify(rabbitMQProducer).sendEnrollmentCancelledEvent(any());
    }

    @Test
    @DisplayName("Test Unenroll - Event Failure Does Not Roll Back Delete")
    void unenroll_EventFailure_StillDeletes() {
        Enrollment enrollment = enrollment(10, 1, 101, "ACTIVE", 25, false);
        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));
        doThrow(new RuntimeException("broker down"))
                .when(rabbitMQProducer).sendEnrollmentCancelledEvent(any());

        enrollmentService.unenroll(10);

        verify(enrollmentRepository).delete(enrollment);
    }

    @Test
    @DisplayName("Test Get Enrollments By Course - Success")
    void getEnrollmentsByCourse_Success() {
        when(enrollmentRepository.findByCourseId(101))
                .thenReturn(List.of(enrollment(10, 1, 101, "ACTIVE", 0, false)));
        when(enrollmentMapper.toDto(any(Enrollment.class)))
                .thenReturn(response(10, 1, 101, "ACTIVE", 0, false));

        List<EnrollmentResponseDto> result = enrollmentService.getEnrollmentsByCourse(101);

        assertEquals(1, result.size());
        assertEquals(101, result.get(0).getCourseId());
    }

    @Test
    @DisplayName("Test Get Enrollment By Id - Not Found")
    void getEnrollmentById_NotFound() {
        when(enrollmentRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> enrollmentService.getEnrollmentById(99));
    }

    @Test
    @DisplayName("Test Update Progress - Completes At 100 Percent")
    void updateProgress_CompleteAtOneHundred() {
        Enrollment enrollment = enrollment(10, 1, 101, "ACTIVE", 25, false);
        EnrollmentResponseDto response = response(10, 1, 101, "COMPLETED", 100, false);

        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(enrollmentMapper.toDto(enrollment)).thenReturn(response);

        EnrollmentResponseDto result = enrollmentService.updateProgress(10, 100);

        assertEquals("COMPLETED", enrollment.getStatus());
        assertEquals(100, enrollment.getProgressPercent());
        assertNotNull(enrollment.getCompletedAt());
        assertEquals("COMPLETED", result.getStatus());
    }

    @Test
    @DisplayName("Test Update Progress - Rejects Out Of Range")
    void updateProgress_OutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> enrollmentService.updateProgress(10, 101));
        verify(enrollmentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Test Mark Complete - Success")
    void markComplete_Success() {
        Enrollment enrollment = enrollment(10, 1, 101, "ACTIVE", 40, false);
        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(enrollmentMapper.toDto(enrollment))
                .thenReturn(response(10, 1, 101, "COMPLETED", 100, false));

        EnrollmentResponseDto result = enrollmentService.markComplete(10);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(100, enrollment.getProgressPercent());
        assertNotNull(enrollment.getCompletedAt());
    }

    @Test
    @DisplayName("Test Is Enrolled - Delegates To Repository")
    void isEnrolled_DelegatesToRepository() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(1, 101)).thenReturn(true);

        assertTrue(enrollmentService.isEnrolled(1, 101));
    }

    @Test
    @DisplayName("Test Get Enrollment By Student And Course - Missing Returns Null")
    void getEnrollmentByStudentAndCourse_Missing_ReturnsNull() {
        when(enrollmentRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());

        assertNull(enrollmentService.getEnrollmentByStudentAndCourse(1, 101));
    }

    @Test
    @DisplayName("Test Issue Certificate - Success")
    void issueCertificate_Success() {
        Enrollment enrollment = enrollment(10, 1, 101, "COMPLETED", 100, false);
        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(enrollmentMapper.toDto(enrollment))
                .thenReturn(response(10, 1, 101, "COMPLETED", 100, true));

        EnrollmentResponseDto result = enrollmentService.issueCertificate(10);

        assertTrue(enrollment.getCertificateIssued());
        assertTrue(result.getCertificateIssued());
    }

    @Test
    @DisplayName("Test Issue Certificate - Requires Completed Enrollment")
    void issueCertificate_NotCompleted() {
        when(enrollmentRepository.findById(10))
                .thenReturn(Optional.of(enrollment(10, 1, 101, "ACTIVE", 50, false)));

        assertThrows(IllegalStateException.class, () -> enrollmentService.issueCertificate(10));
    }

    @Test
    @DisplayName("Test Issue Certificate - Rejects Duplicate Certificate")
    void issueCertificate_AlreadyIssued() {
        when(enrollmentRepository.findById(10))
                .thenReturn(Optional.of(enrollment(10, 1, 101, "COMPLETED", 100, true)));

        assertThrows(IllegalStateException.class, () -> enrollmentService.issueCertificate(10));
    }

    @Test
    @DisplayName("Test Get Enrollment Count - Success")
    void getEnrollmentCount_Success() {
        when(enrollmentRepository.countByCourseId(101)).thenReturn(7L);

        assertEquals(7L, enrollmentService.getEnrollmentCount(101));
    }

    @Test
    @DisplayName("Test Cancel Enrollment - Saves Cancelled Status")
    void cancelEnrollment_SavesCancelledStatus() {
        Enrollment enrollment = enrollment(10, 1, 101, "ACTIVE", 30, false);
        when(enrollmentRepository.findById(10)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

        enrollmentService.cancelEnrollment(10);

        assertEquals("CANCELLED", enrollment.getStatus());
        verify(enrollmentRepository).save(enrollment);
    }

    @Test
    @DisplayName("Test Get All Enrollments - Success")
    void getAllEnrollments_Success() {
        when(enrollmentRepository.findAll())
                .thenReturn(List.of(enrollment(10, 1, 101, "ACTIVE", 0, false)));
        when(enrollmentMapper.toDto(any(Enrollment.class)))
                .thenReturn(response(10, 1, 101, "ACTIVE", 0, false));

        List<EnrollmentResponseDto> result = enrollmentService.getAllEnrollments();

        assertEquals(1, result.size());
    }

    private Enrollment enrollment(
            Integer enrollmentId,
            Integer studentId,
            Integer courseId,
            String status,
            Integer progressPercent,
            Boolean certificateIssued) {
        Enrollment enrollment = new Enrollment();
        enrollment.setEnrollmentId(enrollmentId);
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(status);
        enrollment.setProgressPercent(progressPercent);
        enrollment.setCertificateIssued(certificateIssued);
        return enrollment;
    }

    private EnrollmentResponseDto response(
            Integer enrollmentId,
            Integer studentId,
            Integer courseId,
            String status,
            Integer progressPercent,
            Boolean certificateIssued) {
        EnrollmentResponseDto response = new EnrollmentResponseDto();
        response.setEnrollmentId(enrollmentId);
        response.setStudentId(studentId);
        response.setCourseId(courseId);
        response.setStatus(status);
        response.setProgressPercent(progressPercent);
        response.setCertificateIssued(certificateIssued);
        return response;
    }
}

package com.edulearn.enrollment.service.impl;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.entity.Enrollment;
import com.edulearn.enrollment.exception.AlreadyEnrolledException;
import com.edulearn.enrollment.exception.EnrollmentNotFoundException;
import com.edulearn.enrollment.mapper.EnrollmentMapper;
import com.edulearn.enrollment.repository.EnrollmentRepository;
import com.edulearn.enrollment.service.EnrollmentService;
import com.edulearn.enrollment.messaging.RabbitMQProducer;
import com.edulearn.enrollment.event.EnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final RabbitMQProducer rabbitMQProducer;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Override
    @Transactional
    public EnrollmentResponseDto enroll(EnrollmentRequestDto request) {
        log.info("Enrolling student {} in course {}", request.getStudentId(), request.getCourseId());

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                request.getStudentId(), request.getCourseId())) {
            throw new AlreadyEnrolledException(
                "Student " + request.getStudentId() +
                " is already enrolled in course " + request.getCourseId());
        }

        // Strict Backend Enforcement of Subscription Limits
        if (Boolean.TRUE.equals(request.getEnrolledViaSubscription())) {
            try {
                String url = "http://payment-service/api/v1/payments/subscriptions/student/" + request.getStudentId();
                com.edulearn.enrollment.dto.SubscriptionDto[] subs = restTemplate.getForObject(url, com.edulearn.enrollment.dto.SubscriptionDto[].class);
                
                com.edulearn.enrollment.dto.SubscriptionDto activeSub = java.util.Arrays.stream(subs != null ? subs : new com.edulearn.enrollment.dto.SubscriptionDto[0])
                        .filter(s -> "ACTIVE".equals(s.getStatus()))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No active premium subscription found to support free enrollment."));

                if (!"MONTHLY".equals(activeSub.getPlanType()) && !"ANNUAL".equals(activeSub.getPlanType())) {
                    throw new IllegalStateException("Your current plan does not support free course enrollment.");
                }

                long currentCount = enrollmentRepository.findByStudentId(request.getStudentId()).stream()
                        .filter(e -> Boolean.TRUE.equals(e.getEnrolledViaSubscription()))
                        .count();

                int limit = "MONTHLY".equals(activeSub.getPlanType()) ? 2 : 5;
                if (currentCount >= limit) {
                    throw new IllegalStateException("Strict limit reached! Your " + activeSub.getPlanType() + " plan only allows " + limit + " free courses.");
                }
                log.info("Strict limit check passed for student {}: {}/{} used", request.getStudentId(), currentCount + 1, limit);
            } catch (Exception e) {
                log.error("Failed to strictly verify subscription limits: {}", e.getMessage());
                if (e instanceof IllegalStateException) throw (IllegalStateException) e;
                throw new IllegalStateException("Inter-service communication failure while verifying subscription. Please try again later.");
            }
        }

        Enrollment enrollment = Enrollment.builder()
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .status("ACTIVE")
                .progressPercent(0)
                .certificateIssued(false)
                .enrolledViaSubscription(request.getEnrolledViaSubscription() != null ? request.getEnrolledViaSubscription() : false)
                .build();

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment created with ID {}", saved.getEnrollmentId());
        
        try {
            rabbitMQProducer.sendEnrollmentCreatedEvent(EnrollmentEvent.builder()
                    .enrollmentId(saved.getEnrollmentId())
                    .studentId(saved.getStudentId())
                    .courseId(saved.getCourseId())
                    .status(saved.getStatus())
                    .build());
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable — enrollment event not published for ID {}: {}", saved.getEnrollmentId(), e.getMessage());
        }
                
        return enrollmentMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void unenroll(Integer enrollmentId) {
        log.info("Unenrolling enrollment ID {}", enrollmentId);
        Enrollment enrollment = getEnrollmentEntity(enrollmentId);
        enrollmentRepository.delete(enrollment);
        
        try {
            rabbitMQProducer.sendEnrollmentCancelledEvent(EnrollmentEvent.builder()
                    .enrollmentId(enrollment.getEnrollmentId())
                    .studentId(enrollment.getStudentId())
                    .courseId(enrollment.getCourseId())
                    .status("CANCELLED")
                    .build());
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable — cancel event not published for enrollment {}: {}", enrollmentId, e.getMessage());
        }
    }

    @Override
    public List<EnrollmentResponseDto> getEnrollmentsByStudent(Integer studentId) {
        log.info("Fetching enrollments for student {}", studentId);
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(enrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentResponseDto> getEnrollmentsByCourse(Integer courseId) {
        log.info("Fetching enrollments for course {}", courseId);
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(enrollmentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EnrollmentResponseDto getEnrollmentById(Integer enrollmentId) {
        return enrollmentMapper.toDto(getEnrollmentEntity(enrollmentId));
    }

    @Override
    @Transactional
    public EnrollmentResponseDto updateProgress(Integer enrollmentId, Integer progressPercent) {
        log.info("Updating progress for enrollment {} to {}%", enrollmentId, progressPercent);

        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("Progress percent must be between 0 and 100");
        }

        Enrollment enrollment = getEnrollmentEntity(enrollmentId);
        enrollment.setProgressPercent(progressPercent);

        if (progressPercent == 100) {
            enrollment.setStatus("COMPLETED");
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        return enrollmentMapper.toDto(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional
    public EnrollmentResponseDto markComplete(Integer enrollmentId) {
        log.info("Marking enrollment {} as complete", enrollmentId);
        Enrollment enrollment = getEnrollmentEntity(enrollmentId);
        enrollment.setStatus("COMPLETED");
        enrollment.setProgressPercent(100);
        enrollment.setCompletedAt(LocalDateTime.now());
        return enrollmentMapper.toDto(enrollmentRepository.save(enrollment));
    }

    @Override
    public boolean isEnrolled(Integer studentId, Integer courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    @Override
    public EnrollmentResponseDto getEnrollmentByStudentAndCourse(Integer studentId, Integer courseId) {
        return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(enrollmentMapper::toDto)
                .orElse(null);
    }

    @Override
    @Transactional
    public EnrollmentResponseDto issueCertificate(Integer enrollmentId) {
        log.info("Issuing certificate for enrollment {}", enrollmentId);
        Enrollment enrollment = getEnrollmentEntity(enrollmentId);

        if (!enrollment.getStatus().equals("COMPLETED")) {
            throw new IllegalStateException(
                "Certificate can only be issued for completed enrollments");
        }

        if (enrollment.getCertificateIssued()) {
            throw new IllegalStateException("Certificate already issued for this enrollment");
        }

        enrollment.setCertificateIssued(true);
        return enrollmentMapper.toDto(enrollmentRepository.save(enrollment));
    }

    @Override
    public long getEnrollmentCount(Integer courseId) {
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional
    public void cancelEnrollment(Integer enrollmentId) {
        log.info("Cancelling enrollment {}", enrollmentId);
        Enrollment enrollment = getEnrollmentEntity(enrollmentId);
        enrollment.setStatus("CANCELLED");
        enrollmentRepository.save(enrollment);
    }

    private Enrollment getEnrollmentEntity(Integer enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                    "Enrollment not found with ID: " + enrollmentId));
    }
    @Override
    public List<EnrollmentResponseDto> getAllEnrollments() {
        log.info("Admin fetching all enrollments");
        return enrollmentRepository.findAll()
                .stream()
                .map(enrollmentMapper::toDto)
                .collect(Collectors.toList());
    }
}
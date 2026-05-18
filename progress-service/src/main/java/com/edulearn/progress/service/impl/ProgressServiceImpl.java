package com.edulearn.progress.service.impl;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.dto.external.EnrollmentDto;
import com.edulearn.progress.dto.external.LessonDto;
import com.edulearn.progress.entity.Progress;
import com.edulearn.progress.mapper.ProgressMapper;
import com.edulearn.progress.repository.ProgressRepository;
import com.edulearn.progress.service.CertificateService;
import com.edulearn.progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressServiceImpl implements ProgressService {

    private final ProgressRepository progressRepository;
    private final ProgressMapper progressMapper;
    private final CertificateService certificateService;
    private final RestTemplate restTemplate;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public ProgressResponseDto watchLesson(WatchProgressRequest request) {
        log.info("Tracking watch progress for student: {}, lesson: {}", request.getStudentId(), request.getLessonId());

        Progress progress = progressRepository.findByStudentIdAndLessonId(request.getStudentId(), request.getLessonId())
                .orElse(Progress.builder()
                        .studentId(request.getStudentId())
                        .courseId(request.getCourseId())
                        .lessonId(request.getLessonId())
                        .watchedSeconds(0)
                        .isCompleted(false)
                        .build());

        // Update watched seconds
        if (request.getWatchedSeconds() > progress.getWatchedSeconds()) {
            progress.setWatchedSeconds(request.getWatchedSeconds());
        }

        // Logical Check: Should it be marked complete?
        if (!progress.getIsCompleted()) {
            checkAndMarkComplete(progress);
        }

        // Single Save
        Progress saved = progressRepository.save(progress);
        
        // FORCE FLUSH to catch DB errors here instead of during commit
        entityManager.flush();
        
        // Non-critical external notifications
        try {
            updateCourseProgress(request.getStudentId(), request.getCourseId());
        } catch (Exception e) {
            log.error("Failed to notify external services: {}", e.getMessage());
        }

        return progressMapper.toDto(saved);
    }

    private void checkAndMarkComplete(Progress progress) {
        try {
            // Fetch lesson details to get duration
            List<LessonDto> lessons = restTemplate.exchange(
                    "http://lesson-service/api/v1/lessons/course/" + progress.getCourseId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LessonDto>>() {}
            ).getBody();

            if (lessons != null) {
                lessons.stream()
                        .filter(l -> l.getLessonId().equals(progress.getLessonId()))
                        .findFirst()
                        .ifPresent(lesson -> {
                            int durationSeconds = lesson.getDurationMinutes() * 60;
                            // Threshold updated to 50% (0.5) to match frontend requirement
                            if (progress.getWatchedSeconds() >= durationSeconds * 0.5) {
                                progress.setIsCompleted(true);
                                progress.setCompletedAt(LocalDateTime.now());
                                log.info("Lesson {} marked as COMPLETED for student {}", progress.getLessonId(), progress.getStudentId());
                            }
                        });
            }
        } catch (Exception e) {
            log.error("Error checking lesson completion", e);
        }
    }

    private void updateCourseProgress(Integer studentId, Integer courseId) {
        try {
            // Get total lessons
            List<LessonDto> lessons = restTemplate.exchange(
                    "http://lesson-service/api/v1/lessons/course/" + courseId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LessonDto>>() {}
            ).getBody();

            if (lessons == null || lessons.isEmpty()) return;

            int totalLessons = lessons.size();

            // Get completed lessons
            List<Progress> progressList = progressRepository.findByStudentIdAndCourseId(studentId, courseId);
            long completedLessons = progressList.stream().filter(Progress::getIsCompleted).count();

            double progressPercent = ((double) completedLessons / totalLessons) * 100;
            // Round to 2 decimal places
            progressPercent = Math.round(progressPercent * 100.0) / 100.0;

            // Get enrollment ID (using Map for safety)
            Map<String, Object> enrollmentRes = restTemplate.getForObject(
                    "http://enrollment-service/api/v1/enrollments/check?studentId=" + studentId + "&courseId=" + courseId,
                    Map.class
            );

            if (enrollmentRes != null && Boolean.TRUE.equals(enrollmentRes.get("enrolled"))) {
                Integer enrollmentId = (Integer) enrollmentRes.get("enrollmentId");
                // Update enrollment service
                restTemplate.put(
                        "http://enrollment-service/api/v1/enrollments/" + enrollmentId + "/progress?progressPercent=" + (int) Math.round(progressPercent),
                        null
                );
                log.info("Updated enrollment {} progress to {}%", enrollmentId, progressPercent);

                // Auto issue certificate if 100%
                if (progressPercent >= 100.0) {
                    certificateService.generateCertificate(studentId, courseId);
                    log.info("Course 100% complete. Certificate triggered for student {} and course {}", studentId, courseId);
                }
            }
        } catch (Exception e) {
            log.error("Error updating course progress", e);
        }
    }

    @Override
    public Double getCourseProgressPercent(Integer studentId, Integer courseId) {
        try {
            List<LessonDto> lessons = restTemplate.exchange(
                    "http://lesson-service/api/v1/lessons/course/" + courseId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<LessonDto>>() {}
            ).getBody();

            if (lessons == null || lessons.isEmpty()) return 0.0;

            long completed = progressRepository.findByStudentIdAndCourseId(studentId, courseId)
                    .stream().filter(Progress::getIsCompleted).count();

            return Math.round(((double) completed / lessons.size()) * 10000.0) / 100.0;
        } catch (Exception e) {
            log.error("Error calculating course progress percent", e);
            return 0.0;
        }
    }

    @Override
    public List<ProgressResponseDto> getStudentProgress(Integer studentId) {
        return progressRepository.findByStudentId(studentId).stream()
                .map(progressMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProgressResponseDto> getStudentProgress(Integer studentId, Integer courseId) {
        log.info("Fetching progress for student {} in course {}", studentId, courseId);
        return progressRepository.findByStudentIdAndCourseId(studentId, courseId).stream()
                .map(progressMapper::toDto)
                .collect(Collectors.toList());
    }
}
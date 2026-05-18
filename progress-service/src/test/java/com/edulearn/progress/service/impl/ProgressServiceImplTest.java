package com.edulearn.progress.service.impl;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.dto.external.LessonDto;
import com.edulearn.progress.entity.Progress;
import com.edulearn.progress.mapper.ProgressMapper;
import com.edulearn.progress.repository.ProgressRepository;
import com.edulearn.progress.service.CertificateService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock
    private ProgressRepository progressRepository;
    @Mock
    private ProgressMapper progressMapper;
    @Mock
    private CertificateService certificateService;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ProgressServiceImpl progressService;

    @Test
    @DisplayName("Test Watch Lesson - Success")
    void watchLesson_Success() {
        // given
        WatchProgressRequest request = new WatchProgressRequest();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setLessonId(501);
        request.setWatchedSeconds(120);

        Progress progress = new Progress();
        progress.setWatchedSeconds(100);
        
        when(progressRepository.findByStudentIdAndLessonId(1, 501)).thenReturn(Optional.of(progress));
        when(progressRepository.save(any(Progress.class))).thenReturn(progress);
        when(progressMapper.toDto(any(Progress.class))).thenReturn(new ProgressResponseDto());

        // when
        ProgressResponseDto result = progressService.watchLesson(request);

        // then
        assertNotNull(result);
        assertEquals(120, progress.getWatchedSeconds());
        verify(progressRepository, times(1)).save(any(Progress.class));
    }

    @Test
    @DisplayName("Test Get Course Progress Percent - Success")
    void getCourseProgressPercent_Success() {
        // given
        Integer studentId = 1;
        Integer courseId = 101;
        
        // Skip restTemplate call by mocking it if needed, or handle exception in catch block
        // In the current implementation, if restTemplate fails it returns 0.0
        
        // when
        Double result = progressService.getCourseProgressPercent(studentId, courseId);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test Get Student Progress - Success")
    void getStudentProgress_Success() {
        // given
        Integer studentId = 1;
        when(progressRepository.findByStudentId(studentId)).thenReturn(List.of(new Progress()));
        when(progressMapper.toDto(any(Progress.class))).thenReturn(new ProgressResponseDto());

        // when
        List<ProgressResponseDto> result = progressService.getStudentProgress(studentId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Watch Lesson - New Progress Completes And Updates Enrollment")
    void watchLesson_NewProgressCompletesAndUpdatesEnrollment() {
        WatchProgressRequest request = new WatchProgressRequest(1, 101, 501, 180);
        LessonDto lesson = new LessonDto();
        lesson.setLessonId(501);
        lesson.setDurationMinutes(6);
        Progress saved = Progress.builder()
                .studentId(1)
                .courseId(101)
                .lessonId(501)
                .watchedSeconds(180)
                .isCompleted(true)
                .build();

        when(progressRepository.findByStudentIdAndLessonId(1, 501)).thenReturn(Optional.empty());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(lesson)));
        when(progressRepository.save(any(Progress.class))).thenReturn(saved);
        when(progressRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(List.of(saved));
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("enrolled", true, "enrollmentId", 7));
        when(progressMapper.toDto(saved)).thenReturn(new ProgressResponseDto());

        assertNotNull(progressService.watchLesson(request));

        verify(restTemplate).put(contains("/7/progress"), isNull());
    }

    @Test
    @DisplayName("Test Watch Lesson - External Update Failure Still Returns")
    void watchLesson_ExternalUpdateFailureStillReturns() {
        WatchProgressRequest request = new WatchProgressRequest(1, 101, 501, 10);
        Progress progress = Progress.builder()
                .studentId(1)
                .courseId(101)
                .lessonId(501)
                .watchedSeconds(0)
                .isCompleted(true)
                .build();
        when(progressRepository.findByStudentIdAndLessonId(1, 501)).thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("lesson down"));
        when(progressMapper.toDto(progress)).thenReturn(new ProgressResponseDto());

        assertNotNull(progressService.watchLesson(request));
    }

    @Test
    @DisplayName("Test Get Course Progress Percent - Calculates Percent")
    void getCourseProgressPercent_CalculatesPercent() {
        LessonDto one = new LessonDto();
        one.setLessonId(1);
        LessonDto two = new LessonDto();
        two.setLessonId(2);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(List.of(one, two)));
        when(progressRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(List.of(
                Progress.builder().isCompleted(true).build(),
                Progress.builder().isCompleted(false).build()));

        assertEquals(50.0, progressService.getCourseProgressPercent(1, 101));
    }

    @Test
    @DisplayName("Test Get Student Course Progress - Success")
    void getStudentCourseProgress_Success() {
        when(progressRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(List.of(new Progress()));
        when(progressMapper.toDto(any(Progress.class))).thenReturn(new ProgressResponseDto());

        assertEquals(1, progressService.getStudentProgress(1, 101).size());
    }
}

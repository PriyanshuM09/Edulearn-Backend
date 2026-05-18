package com.edulearn.lesson.service.impl;

import com.edulearn.lesson.dto.LessonRequestDto;
import com.edulearn.lesson.dto.LessonResponseDto;
import com.edulearn.lesson.dto.ResourceRequestDto;
import com.edulearn.lesson.dto.ResourceResponseDto;
import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.exception.LessonNotFoundException;
import com.edulearn.lesson.exception.ResourceNotFoundException;
import com.edulearn.lesson.mapper.LessonMapper;
import com.edulearn.lesson.repository.LessonRepository;
import com.edulearn.lesson.repository.ResourceRepository;
import com.edulearn.lesson.mapper.ResourceMapper;
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
class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private ResourceMapper resourceMapper;

    @InjectMocks
    private LessonServiceImpl lessonService;

    @Test
    @DisplayName("Test Add Lesson - Success")
    void addLesson_Success() {
        // given
        LessonRequestDto request = new LessonRequestDto();
        request.setTitle("Intro to Java");
        request.setCourseId(1);
        request.setContentType("VIDEO");
        request.setOrderIndex(1);

        Lesson lesson = new Lesson();
        lesson.setLessonId(1);
        LessonResponseDto response = new LessonResponseDto();
        response.setLessonId(1);

        when(lessonMapper.toEntity(any(LessonRequestDto.class))).thenReturn(lesson);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);
        when(lessonMapper.toDto(any(Lesson.class))).thenReturn(response);

        // when
        LessonResponseDto result = lessonService.addLesson(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.getLessonId());
        verify(lessonRepository, times(1)).save(any(Lesson.class));
    }

    @Test
    @DisplayName("Test Get Lessons By Course - Success")
    void getLessonsByCourse_Success() {
        // given
        Integer courseId = 1;
        when(lessonRepository.findByCourseIdOrderByOrderIndex(courseId)).thenReturn(List.of(new Lesson()));
        when(lessonMapper.toDtoList(anyList())).thenReturn(List.of(new LessonResponseDto()));

        // when
        List<LessonResponseDto> result = lessonService.getLessonsByCourse(courseId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Get Lesson By ID - Success")
    void getLessonById_Success() {
        // given
        Integer id = 1;
        Lesson lesson = new Lesson();
        LessonResponseDto response = new LessonResponseDto();

        when(lessonRepository.findById(id)).thenReturn(Optional.of(lesson));
        when(lessonMapper.toDto(lesson)).thenReturn(response);

        // when
        LessonResponseDto result = lessonService.getLessonById(id);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test Get Lesson By ID - Not Found")
    void getLessonById_NotFound() {
        // given
        Integer id = 99;
        when(lessonRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThrows(LessonNotFoundException.class, () -> lessonService.getLessonById(id));
    }

    @Test
    void updateLesson_Success() {
        LessonRequestDto request = new LessonRequestDto();
        Lesson lesson = new Lesson();
        Lesson saved = new Lesson();
        LessonResponseDto response = new LessonResponseDto();

        when(lessonRepository.findById(1)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(lesson)).thenReturn(saved);
        when(lessonMapper.toDto(saved)).thenReturn(response);

        assertSame(response, lessonService.updateLesson(1, request));
        verify(lessonMapper).updateEntityFromDto(request, lesson);
    }

    @Test
    void deleteLesson_SuccessAndNotFound() {
        when(lessonRepository.existsById(1)).thenReturn(true);
        lessonService.deleteLesson(1);
        verify(lessonRepository).deleteById(1);

        when(lessonRepository.existsById(99)).thenReturn(false);
        assertThrows(LessonNotFoundException.class, () -> lessonService.deleteLesson(99));
    }

    @Test
    void reorderLessons_SuccessAndNotFound() {
        Lesson first = new Lesson();
        Lesson second = new Lesson();
        when(lessonRepository.findById(1)).thenReturn(Optional.of(first));
        when(lessonRepository.findById(2)).thenReturn(Optional.of(second));

        lessonService.reorderLessons(10, List.of(1, 2));

        assertEquals(0, first.getOrderIndex());
        assertEquals(1, second.getOrderIndex());
        verify(lessonRepository).save(first);
        verify(lessonRepository).save(second);

        when(lessonRepository.findById(3)).thenReturn(Optional.empty());
        List<Integer> missingLessonOrder = List.of(3);
        assertThrows(LessonNotFoundException.class, () -> lessonService.reorderLessons(10, missingLessonOrder));
    }

    @Test
    void addResource_SuccessAndMissingLesson() {
        Lesson lesson = new Lesson();
        ResourceRequestDto request = new ResourceRequestDto();
        request.setName("Slides");
        Resource resource = new Resource();
        Resource saved = new Resource();
        ResourceResponseDto response = new ResourceResponseDto();

        when(lessonRepository.findById(1)).thenReturn(Optional.of(lesson));
        when(resourceMapper.toEntity(request)).thenReturn(resource);
        when(resourceRepository.save(resource)).thenReturn(saved);
        when(resourceMapper.toDto(saved)).thenReturn(response);

        assertSame(response, lessonService.addResource(1, request));
        assertSame(lesson, resource.getLesson());

        when(lessonRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(LessonNotFoundException.class, () -> lessonService.addResource(99, request));
    }

    @Test
    void removeResource_SuccessAndMissingBranches() {
        Resource resource = new Resource();
        when(lessonRepository.existsById(1)).thenReturn(true);
        when(resourceRepository.findById(5)).thenReturn(Optional.of(resource));

        lessonService.removeResource(1, 5);

        verify(resourceRepository).delete(resource);

        when(lessonRepository.existsById(99)).thenReturn(false);
        assertThrows(LessonNotFoundException.class, () -> lessonService.removeResource(99, 5));

        when(lessonRepository.existsById(1)).thenReturn(true);
        when(resourceRepository.findById(404)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> lessonService.removeResource(1, 404));
    }

    @Test
    void getPreviewLessons_FiltersOnlyPreviewLessons() {
        Lesson preview = new Lesson();
        preview.setIsPreview(true);
        Lesson locked = new Lesson();
        locked.setIsPreview(false);
        List<LessonResponseDto> response = List.of(new LessonResponseDto());

        when(lessonRepository.findByCourseId(1)).thenReturn(List.of(preview, locked));
        when(lessonMapper.toDtoList(List.of(preview))).thenReturn(response);

        assertEquals(1, lessonService.getPreviewLessons(1).size());
    }
}

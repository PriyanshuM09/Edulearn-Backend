package com.edulearn.lesson.service.impl;

import com.edulearn.lesson.dto.*;
import com.edulearn.lesson.entity.Lesson;
import com.edulearn.lesson.entity.Resource;
import com.edulearn.lesson.exception.LessonNotFoundException;
import com.edulearn.lesson.exception.ResourceNotFoundException;
import com.edulearn.lesson.mapper.LessonMapper;
import com.edulearn.lesson.mapper.ResourceMapper;
import com.edulearn.lesson.repository.LessonRepository;
import com.edulearn.lesson.repository.ResourceRepository;
import com.edulearn.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ResourceRepository resourceRepository;
    private final LessonMapper lessonMapper;
    private final ResourceMapper resourceMapper;

    @Override
    @Transactional
    public LessonResponseDto addLesson(LessonRequestDto requestDto) {
        log.info("Adding lesson '{}' to course ID: {}", requestDto.getTitle(), requestDto.getCourseId());
        Lesson lesson = lessonMapper.toEntity(requestDto);
        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson created with ID: {}", saved.getLessonId());
        return lessonMapper.toDto(saved);
    }

    @Override
    public List<LessonResponseDto> getLessonsByCourse(Integer courseId) {
        log.info("Fetching lessons for course ID: {}", courseId);
        return lessonMapper.toDtoList(
                lessonRepository.findByCourseIdOrderByOrderIndex(courseId));
    }

    @Override
    public LessonResponseDto getLessonById(Integer lessonId) {
        log.info("Fetching lesson with ID: {}", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
        return lessonMapper.toDto(lesson);
    }

    @Override
    @Transactional
    public LessonResponseDto updateLesson(Integer lessonId, LessonRequestDto requestDto) {
        log.info("Updating lesson with ID: {}", lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
        lessonMapper.updateEntityFromDto(requestDto, lesson);
        return lessonMapper.toDto(lessonRepository.save(lesson));
    }

    @Override
    @Transactional
    public void deleteLesson(Integer lessonId) {
        log.info("Deleting lesson with ID: {}", lessonId);
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        lessonRepository.deleteById(lessonId);
        log.info("Lesson {} deleted successfully", lessonId);
    }

    @Override
    @Transactional
    public void reorderLessons(Integer courseId, List<Integer> orderedLessonIds) {
        log.info("Reordering {} lessons for course ID: {}", orderedLessonIds.size(), courseId);
        AtomicInteger index = new AtomicInteger(0);
        orderedLessonIds.forEach(lessonId -> {
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new LessonNotFoundException(lessonId));
            lesson.setOrderIndex(index.getAndIncrement());
            lessonRepository.save(lesson);
        });
        log.info("Reorder complete for course ID: {}", courseId);
    }

    @Override
    @Transactional
    public ResourceResponseDto addResource(Integer lessonId, ResourceRequestDto requestDto) {
        log.info("Adding resource '{}' to lesson ID: {}", requestDto.getName(), lessonId);
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new LessonNotFoundException(lessonId));
        Resource resource = resourceMapper.toEntity(requestDto);
        resource.setLesson(lesson);
        Resource saved = resourceRepository.save(resource);
        log.info("Resource created with ID: {}", saved.getResourceId());
        return resourceMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void removeResource(Integer lessonId, Integer resourceId) {
        log.info("Removing resource ID: {} from lesson ID: {}", resourceId, lessonId);
        if (!lessonRepository.existsById(lessonId)) {
            throw new LessonNotFoundException(lessonId);
        }
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException(resourceId));
        resourceRepository.delete(resource);
        log.info("Resource {} removed successfully", resourceId);
    }

    @Override
    public List<LessonResponseDto> getPreviewLessons(Integer courseId) {
        log.info("Fetching preview lessons for course ID: {}", courseId);
        List<Lesson> previews = lessonRepository.findByCourseId(courseId)
                .stream()
                .filter(Lesson::getIsPreview)
                .collect(Collectors.toList());
        return lessonMapper.toDtoList(previews);
    }
}
package com.edulearn.lesson.resource;

import com.edulearn.lesson.dto.*;
import com.edulearn.lesson.service.LessonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
@Tag(name = "Lesson API", description = "Lesson CRUD, ordering, resource attachment, and preview operations")
public class LessonResource {

    private final LessonService lessonService;

    // POST /api/v1/lessons
    @PostMapping
    @Operation(summary = "Add a new lesson to a course",
               responses = @ApiResponse(responseCode = "201", description = "Lesson created"))
    public ResponseEntity<LessonResponseDto> addLesson(
            @Valid @RequestBody LessonRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.addLesson(requestDto));
    }

    // GET /api/v1/lessons/course/{courseId}
    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all lessons for a course ordered by orderIndex")
    public ResponseEntity<List<LessonResponseDto>> getLessonsByCourse(
            @Parameter(description = "Course ID") @PathVariable Integer courseId) {
        return ResponseEntity.ok(lessonService.getLessonsByCourse(courseId));
    }

    // GET /api/v1/lessons/{lessonId}
    @GetMapping("/{lessonId}")
    @Operation(summary = "Get lesson by ID")
    public ResponseEntity<LessonResponseDto> getLessonById(
            @PathVariable Integer lessonId) {
        return ResponseEntity.ok(lessonService.getLessonById(lessonId));
    }

    // PUT /api/v1/lessons/{lessonId}
    @PutMapping("/{lessonId}")
    @Operation(summary = "Update a lesson")
    public ResponseEntity<LessonResponseDto> updateLesson(
            @PathVariable Integer lessonId,
            @Valid @RequestBody LessonRequestDto requestDto) {
        return ResponseEntity.ok(lessonService.updateLesson(lessonId, requestDto));
    }

    // DELETE /api/v1/lessons/{lessonId}
    @DeleteMapping("/{lessonId}")
    @Operation(summary = "Delete a lesson",
               responses = @ApiResponse(responseCode = "204", description = "Lesson deleted"))
    public ResponseEntity<Void> deleteLesson(@PathVariable Integer lessonId) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/v1/lessons/course/{courseId}/reorder
    @PutMapping("/course/{courseId}/reorder")
    @Operation(summary = "Reorder lessons within a course — pass ordered list of lesson IDs")
    public ResponseEntity<Void> reorderLessons(
            @PathVariable Integer courseId,
            @RequestBody List<Integer> orderedLessonIds) {
        lessonService.reorderLessons(courseId, orderedLessonIds);
        return ResponseEntity.ok().build();
    }

    // POST /api/v1/lessons/{lessonId}/resources
    @PostMapping("/{lessonId}/resources")
    @Operation(summary = "Attach a resource to a lesson",
               responses = @ApiResponse(responseCode = "201", description = "Resource attached"))
    public ResponseEntity<ResourceResponseDto> addResource(
            @PathVariable Integer lessonId,
            @Valid @RequestBody ResourceRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.addResource(lessonId, requestDto));
    }

    // DELETE /api/v1/lessons/{lessonId}/resources/{resourceId}
    @DeleteMapping("/{lessonId}/resources/{resourceId}")
    @Operation(summary = "Remove a resource from a lesson",
               responses = @ApiResponse(responseCode = "204", description = "Resource removed"))
    public ResponseEntity<Void> removeResource(
            @PathVariable Integer lessonId,
            @PathVariable Integer resourceId) {
        lessonService.removeResource(lessonId, resourceId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/lessons/course/{courseId}/preview
    @GetMapping("/course/{courseId}/preview")
    @Operation(summary = "Get free preview lessons for a course")
    public ResponseEntity<List<LessonResponseDto>> getPreviewLessons(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(lessonService.getPreviewLessons(courseId));
    }
}
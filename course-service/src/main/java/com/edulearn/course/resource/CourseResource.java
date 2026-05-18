package com.edulearn.course.resource;

import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.dto.RejectionRequest;
import com.edulearn.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "Course management and publishing lifecycle")
public class CourseResource {

    private final CourseService courseService;

    @PostMapping
    @Operation(summary = "Create a new course (PENDING by default)")
    public ResponseEntity<CourseResponseDto> createCourse(@Valid @RequestBody CourseRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(requestDto));
    }

    @GetMapping
    @Operation(summary = "Public Catalog — Get all APPROVED and PUBLISHED courses")
    public ResponseEntity<List<CourseResponseDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID")
    public ResponseEntity<CourseResponseDto> getCourseById(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/instructor/{instructorId}")
    @Operation(summary = "Instructor — Get all courses owned by instructor")
    public ResponseEntity<List<CourseResponseDto>> getCoursesByInstructor(@PathVariable Integer instructorId) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(instructorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update course details")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Integer id, @Valid @RequestBody CourseRequestDto requestDto) {
        return ResponseEntity.ok(courseService.updateCourse(id, requestDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course")
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // ── NEW PUBLISHING FLOW ──────────────────────────────────────────────

    @PutMapping("/{id}/submit-for-review")
    @Operation(summary = "Instructor — Submit course for admin review")
    public ResponseEntity<CourseResponseDto> submitForReview(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.submitForReview(id));
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "Instructor — Unpublish own course (sets back to PENDING)")
    public ResponseEntity<CourseResponseDto> unpublishCourse(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.unpublishCourse(id));
    }

    @PutMapping("/admin/{id}/approve")
    @Operation(summary = "Admin — Approve and publish course")
    public ResponseEntity<CourseResponseDto> approveCourse(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.approveCourse(id));
    }

    @PutMapping("/admin/{id}/reject")
    @Operation(summary = "Admin — Reject course with reason")
    public ResponseEntity<CourseResponseDto> rejectCourse(@PathVariable Integer id, @RequestBody RejectionRequest rejectionRequest) {
        return ResponseEntity.ok(courseService.rejectCourse(id, rejectionRequest));
    }

    @GetMapping("/admin/pending")
    @Operation(summary = "Admin — List all courses pending review")
    public ResponseEntity<List<CourseResponseDto>> getPendingCourses() {
        return ResponseEntity.ok(courseService.getPendingCourses());
    }

    @GetMapping("/admin/all")
    @Operation(summary = "Admin — List all courses in the system")
    public ResponseEntity<List<CourseResponseDto>> getAllCoursesForAdmin() {
        return ResponseEntity.ok(courseService.getAllCoursesForAdmin());
    }

    @GetMapping("/search")
    @Operation(summary = "Search approved courses")
    public ResponseEntity<List<CourseResponseDto>> searchCourses(@RequestParam String keyword) {
        return ResponseEntity.ok(courseService.searchCourses(keyword));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Filter approved courses by category")
    public ResponseEntity<List<CourseResponseDto>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(courseService.getCoursesByCategory(category));
    }
}
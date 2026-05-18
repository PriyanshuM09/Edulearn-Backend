package com.edulearn.enrollment.resource;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Service", description = "APIs for managing course enrollments")
public class EnrollmentResource {

    private final EnrollmentService enrollmentService;

    // POST /api/v1/enrollments
    @PostMapping
    @Operation(summary = "Enroll a student in a course")
    public ResponseEntity<EnrollmentResponseDto> enroll(
            @Valid @RequestBody EnrollmentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(request));
    }

    // DELETE /api/v1/enrollments/{enrollmentId}
    @DeleteMapping("/{enrollmentId}")
    @Operation(summary = "Unenroll a student from a course")
    public ResponseEntity<Map<String, String>> unenroll(
            @PathVariable Integer enrollmentId) {
        enrollmentService.unenroll(enrollmentId);
        return ResponseEntity.ok(Map.of("message", "Unenrolled successfully"));
    }

    // GET /api/v1/enrollments/{enrollmentId}
    @GetMapping("/{enrollmentId}")
    @Operation(summary = "Get enrollment by ID")
    public ResponseEntity<EnrollmentResponseDto> getEnrollmentById(
            @PathVariable Integer enrollmentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentById(enrollmentId));
    }

    // GET /api/v1/enrollments/student/{studentId}
    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get all enrollments for a student")
    public ResponseEntity<List<EnrollmentResponseDto>> getByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByStudent(studentId));
    }

    // GET /api/v1/enrollments/course/{courseId}
    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all enrollments for a course")
    public ResponseEntity<List<EnrollmentResponseDto>> getByCourse(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByCourse(courseId));
    }

    // PUT /api/v1/enrollments/{enrollmentId}/progress
    @PutMapping("/{enrollmentId}/progress")
    @Operation(summary = "Update progress percentage for an enrollment")
    public ResponseEntity<EnrollmentResponseDto> updateProgress(
            @PathVariable Integer enrollmentId,
            @RequestParam Integer progressPercent) {
        return ResponseEntity.ok(
                enrollmentService.updateProgress(enrollmentId, progressPercent));
    }

    // PUT /api/v1/enrollments/{enrollmentId}/complete
    @PutMapping("/{enrollmentId}/complete")
    @Operation(summary = "Mark an enrollment as complete")
    public ResponseEntity<EnrollmentResponseDto> markComplete(
            @PathVariable Integer enrollmentId) {
        return ResponseEntity.ok(enrollmentService.markComplete(enrollmentId));
    }

    // PUT /api/v1/enrollments/{enrollmentId}/cancel
    @PutMapping("/{enrollmentId}/cancel")
    @Operation(summary = "Cancel an enrollment")
    public ResponseEntity<Map<String, String>> cancelEnrollment(
            @PathVariable Integer enrollmentId) {
        enrollmentService.cancelEnrollment(enrollmentId);
        return ResponseEntity.ok(Map.of("message", "Enrollment cancelled successfully"));
    }

    @GetMapping("/check")
    @Operation(summary = "Check if a student is enrolled in a course")
    public ResponseEntity<Map<String, Object>> isEnrolled(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId) {
        EnrollmentResponseDto enrollment = enrollmentService.getEnrollmentByStudentAndCourse(studentId, courseId);
        if (enrollment != null) {
            return ResponseEntity.ok(Map.of(
                "enrolled", true,
                "enrollmentId", enrollment.getEnrollmentId()
            ));
        }
        return ResponseEntity.ok(Map.of("enrolled", false));
    }

    // POST /api/v1/enrollments/{enrollmentId}/certificate
    @PostMapping("/{enrollmentId}/certificate")
    @Operation(summary = "Issue certificate for a completed enrollment")
    public ResponseEntity<EnrollmentResponseDto> issueCertificate(
            @PathVariable Integer enrollmentId) {
        return ResponseEntity.ok(enrollmentService.issueCertificate(enrollmentId));
    }

    // GET /api/v1/enrollments/course/{courseId}/count
    @GetMapping("/course/{courseId}/count")
    @Operation(summary = "Get total enrollment count for a course")
    public ResponseEntity<Map<String, Long>> getEnrollmentCount(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(
                Map.of("count", enrollmentService.getEnrollmentCount(courseId)));
    }
 // Admin — Get all enrollments
    @GetMapping("/admin/all")
    @Operation(summary = "Admin — Get all enrollments platform wide")
    public ResponseEntity<List<EnrollmentResponseDto>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
}
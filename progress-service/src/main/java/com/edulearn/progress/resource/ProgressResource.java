package com.edulearn.progress.resource;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.service.ProgressService;
import com.edulearn.progress.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/progress")
@RequiredArgsConstructor
@Tag(name = "Progress", description = "Endpoints for automatic progress tracking")
public class ProgressResource {

    private final ProgressService progressService;
    private final CertificateService certificateService;

    @Operation(summary = "Track video watch time and auto-mark completion")
    @PostMapping("/watch")
    public ResponseEntity<?> watch(@Valid @RequestBody WatchProgressRequest request) {
        try {
            return ResponseEntity.ok(progressService.watchLesson(request));
        } catch (Throwable e) {
            log.error("CRITICAL ERROR in Progress Tracking: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "status", "error",
                        "message", e.getMessage() != null ? e.getMessage() : "Unknown Server Error",
                        "type", e.getClass().getName()
                    ));
        }
    }

    @Operation(summary = "Get overall course progress percentage (Summary for frontend)")
    @GetMapping("/summary/{studentId}/{courseId}")
    public ResponseEntity<ProgressSummary> getProgressSummary(
            @PathVariable Integer studentId, 
            @PathVariable Integer courseId) {
        double percent = progressService.getCourseProgressPercent(studentId, courseId);
        log.info("Progress for Student {} on Course {}: {}%", studentId, courseId, percent);
        
        if (percent >= 100.0) {
            try {
                certificateService.generateCertificate(studentId, courseId);
            } catch (Exception e) {
                log.error("Auto-certificate failed: {}", e.getMessage());
            }
        }
        
        return ResponseEntity.ok(new ProgressSummary(percent));
    }

    @Operation(summary = "Force issue a certificate for a student")
    @PostMapping("/force-certificate")
    public ResponseEntity<?> forceCertificate(@RequestBody Map<String, Integer> payload) {
        Integer studentId = payload.get("studentId");
        Integer courseId = payload.get("courseId");
        
        log.info("Force certificate requested for student {} and course {}", studentId, courseId);
        try {
            certificateService.generateCertificate(studentId, courseId);
            return ResponseEntity.ok(Map.of("message", "Certificate issued successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Get all lesson progress for a student in a course")
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<List<ProgressResponseDto>> getStudentCourseProgress(
            @PathVariable Integer studentId, 
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(progressService.getStudentProgress(studentId, courseId));
    }

    @Data
    @AllArgsConstructor
    public static class ProgressSummary {
        private double completionPercentage;
    }
}
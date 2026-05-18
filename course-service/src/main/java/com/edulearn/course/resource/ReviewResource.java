package com.edulearn.course.resource;

import com.edulearn.course.dto.ReviewDto;
import com.edulearn.course.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "Review API", description = "Endpoints for course reviews")
public class ReviewResource {

    private final ReviewService reviewService;

    @PostMapping("/reviews")
    @Operation(summary = "Add a review to a course")
    public ResponseEntity<ReviewDto> addReview(@RequestBody ReviewDto reviewDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.addReview(reviewDto));
    }

    @GetMapping("/{courseId}/reviews")
    @Operation(summary = "Get all reviews for a course")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable Integer courseId) {
        return ResponseEntity.ok(reviewService.getReviewsByCourseId(courseId));
    }
}

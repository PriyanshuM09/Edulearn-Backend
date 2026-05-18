package com.edulearn.course.service;

import com.edulearn.course.dto.ReviewDto;
import java.util.List;

public interface ReviewService {
    ReviewDto addReview(ReviewDto reviewDto);
    List<ReviewDto> getReviewsByCourseId(Integer courseId);
}

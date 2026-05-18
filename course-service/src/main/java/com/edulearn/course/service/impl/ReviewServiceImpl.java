package com.edulearn.course.service.impl;

import com.edulearn.course.dto.ReviewDto;
import com.edulearn.course.entity.Review;
import com.edulearn.course.mapper.ReviewMapper;
import com.edulearn.course.repository.ReviewRepository;
import com.edulearn.course.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional
    public ReviewDto addReview(ReviewDto reviewDto) {
        Review review = reviewMapper.toEntity(reviewDto);
        Review saved = reviewRepository.save(review);
        return reviewMapper.toDto(saved);
    }

    @Override
    public List<ReviewDto> getReviewsByCourseId(Integer courseId) {
        List<Review> reviews = reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId);
        return reviewMapper.toDtoList(reviews);
    }
}

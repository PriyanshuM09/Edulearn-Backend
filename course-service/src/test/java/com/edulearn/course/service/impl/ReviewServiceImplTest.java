package com.edulearn.course.service.impl;

import com.edulearn.course.dto.ReviewDto;
import com.edulearn.course.entity.Review;
import com.edulearn.course.mapper.ReviewMapper;
import com.edulearn.course.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void addReviewMapsSavesAndReturnsDto() {
        ReviewDto request = new ReviewDto();
        Review review = new Review();
        Review saved = new Review();
        ReviewDto response = new ReviewDto();

        when(reviewMapper.toEntity(request)).thenReturn(review);
        when(reviewRepository.save(review)).thenReturn(saved);
        when(reviewMapper.toDto(saved)).thenReturn(response);

        assertSame(response, reviewService.addReview(request));
        verify(reviewRepository).save(review);
    }

    @Test
    void getReviewsByCourseIdReturnsMappedReviews() {
        Review review = new Review();
        List<Review> reviews = List.of(review);
        List<ReviewDto> response = List.of(new ReviewDto());

        when(reviewRepository.findByCourseIdOrderByCreatedAtDesc(1)).thenReturn(reviews);
        when(reviewMapper.toDtoList(reviews)).thenReturn(response);

        assertEquals(1, reviewService.getReviewsByCourseId(1).size());
    }
}

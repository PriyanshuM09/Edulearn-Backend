package com.edulearn.course.mapper;

import com.edulearn.course.dto.ReviewDto;
import com.edulearn.course.entity.Review;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    Review toEntity(ReviewDto dto);
    ReviewDto toDto(Review review);
    List<ReviewDto> toDtoList(List<Review> reviews);
}

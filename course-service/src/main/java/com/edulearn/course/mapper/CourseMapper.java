package com.edulearn.course.mapper;

import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.entity.Course;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper {

    Course toEntity(CourseRequestDto dto);

    @Mapping(target = "instructorName", ignore = true)
    CourseResponseDto toDto(Course course);

    List<CourseResponseDto> toDtoList(List<Course> courses);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(CourseRequestDto dto, @MappingTarget Course course);
}
package com.edulearn.enrollment.mapper;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "enrollmentId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "progressPercent", ignore = true)
    @Mapping(target = "certificateIssued", ignore = true)
    @Mapping(target = "enrolledAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Enrollment toEntity(EnrollmentRequestDto dto);

    EnrollmentResponseDto toDto(Enrollment enrollment);
}
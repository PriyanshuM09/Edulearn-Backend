package com.edulearn.assessment.mapper;

import com.edulearn.assessment.dto.AttemptResponseDto;
import com.edulearn.assessment.entity.Attempt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttemptMapper {

    @Mapping(target = "resultMessage", ignore = true)
    AttemptResponseDto toDto(Attempt attempt);
}
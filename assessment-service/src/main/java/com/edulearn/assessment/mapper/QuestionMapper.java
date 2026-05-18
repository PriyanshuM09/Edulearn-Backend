package com.edulearn.assessment.mapper;

import com.edulearn.assessment.dto.QuestionRequestDto;
import com.edulearn.assessment.dto.QuestionResponseDto;
import com.edulearn.assessment.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(target = "questionId", ignore = true)
    @Mapping(target = "quiz", ignore = true)
    Question toEntity(QuestionRequestDto dto);

    @Mapping(target = "quizId",
             source = "quiz.quizId")
    QuestionResponseDto toDto(Question question);
}
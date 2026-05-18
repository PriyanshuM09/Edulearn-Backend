package com.edulearn.assessment.mapper;

import com.edulearn.assessment.dto.QuizRequestDto;
import com.edulearn.assessment.dto.QuizResponseDto;
import com.edulearn.assessment.entity.Quiz;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {QuestionMapper.class})
public interface QuizMapper {

    @Mapping(target = "quizId", ignore = true)
    @Mapping(target = "isPublished", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Quiz toEntity(QuizRequestDto dto);

    QuizResponseDto toDto(Quiz quiz);
}
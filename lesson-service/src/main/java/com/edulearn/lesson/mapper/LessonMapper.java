package com.edulearn.lesson.mapper;

import com.edulearn.lesson.dto.LessonRequestDto;
import com.edulearn.lesson.dto.LessonResponseDto;
import com.edulearn.lesson.entity.Lesson;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {ResourceMapper.class})
public interface LessonMapper {

    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resources", ignore = true)
    Lesson toEntity(LessonRequestDto dto);

    LessonResponseDto toDto(Lesson lessonEntity);

    List<LessonResponseDto> toDtoList(List<Lesson> lessons);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "lessonId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "resources", ignore = true)
    void updateEntityFromDto(LessonRequestDto dto, @MappingTarget Lesson lessonEntity);
}
package com.edulearn.lesson.mapper;

import com.edulearn.lesson.dto.ResourceRequestDto;
import com.edulearn.lesson.dto.ResourceResponseDto;
import com.edulearn.lesson.entity.Resource;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        builder = @Builder(disableBuilder = true),
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ResourceMapper {

    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "resourceId", ignore = true)
    Resource toEntity(ResourceRequestDto dto);

    @Mapping(source = "lesson.lessonId", target = "lessonId")
    ResourceResponseDto toDto(Resource resourceEntity);

    List<ResourceResponseDto> toDtoList(List<Resource> resources);
}
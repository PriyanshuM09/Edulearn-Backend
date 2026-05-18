package com.edulearn.progress.mapper;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.entity.Progress;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE)
public interface ProgressMapper {

    @Mapping(target = "progressId", ignore = true)
    @Mapping(target = "isCompleted", ignore = true)
    @Mapping(target = "lastAccessedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    Progress toEntity(WatchProgressRequest dto);

    @Mapping(source = "isCompleted", target = "completed")
    ProgressResponseDto toDto(Progress progress);

    List<ProgressResponseDto> toDtoList(List<Progress> progressList);
}
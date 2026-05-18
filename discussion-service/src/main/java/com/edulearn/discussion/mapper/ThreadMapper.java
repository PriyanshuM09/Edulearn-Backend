package com.edulearn.discussion.mapper;

import com.edulearn.discussion.dto.ThreadRequestDto;
import com.edulearn.discussion.dto.ThreadResponseDto;
import com.edulearn.discussion.entity.Thread;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE,
        uses = {ReplyMapper.class})
public interface ThreadMapper {

    @Mapping(target = "replies", ignore = true)
    Thread toEntity(ThreadRequestDto dto);

    @Mapping(target = "replyCount",
             expression = "java(thread.getReplies() != null ? thread.getReplies().size() : 0)")
    ThreadResponseDto toDto(Thread thread);

    List<ThreadResponseDto> toDtoList(List<Thread> threads);

    @BeanMapping(nullValuePropertyMappingStrategy =
            NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ThreadRequestDto dto,
                             @MappingTarget Thread thread);
}
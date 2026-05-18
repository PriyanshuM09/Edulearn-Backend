package com.edulearn.discussion.mapper;

import com.edulearn.discussion.dto.ReplyRequestDto;
import com.edulearn.discussion.dto.ReplyResponseDto;
import com.edulearn.discussion.entity.Reply;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy =
                NullValuePropertyMappingStrategy.IGNORE)
public interface ReplyMapper {

    @Mapping(target = "thread", ignore = true)
    Reply toEntity(ReplyRequestDto dto);

    @Mapping(source = "thread.threadId", target = "threadId")
    ReplyResponseDto toDto(Reply reply);

    List<ReplyResponseDto> toDtoList(List<Reply> replies);
}
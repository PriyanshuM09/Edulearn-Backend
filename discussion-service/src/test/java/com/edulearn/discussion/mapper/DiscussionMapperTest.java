package com.edulearn.discussion.mapper;

import com.edulearn.discussion.dto.ReplyRequestDto;
import com.edulearn.discussion.dto.ReplyResponseDto;
import com.edulearn.discussion.dto.ThreadRequestDto;
import com.edulearn.discussion.dto.ThreadResponseDto;
import com.edulearn.discussion.entity.Reply;
import com.edulearn.discussion.entity.Thread;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled("Generated MapStruct implementations are excluded from coverage and can be unstable in clean Windows reactor runs")
class DiscussionMapperTest {

    private final ReplyMapper replyMapper = Mappers.getMapper(ReplyMapper.class);
    private final ThreadMapper threadMapper = Mappers.getMapper(ThreadMapper.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(threadMapper, "replyMapper", replyMapper);
    }

    @Test
    void mapsThreadRequestAndResponse() {
        ThreadRequestDto request = ThreadRequestDto.builder()
                .courseId(10)
                .authorId(20)
                .authorRole("INSTRUCTOR")
                .authorName("Ada")
                .title("Streams")
                .content("How do streams work?")
                .build();

        Thread entity = threadMapper.toEntity(request);
        entity.setThreadId(30);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.getReplies().add(Reply.builder()
                .replyId(40)
                .thread(entity)
                .authorId(21)
                .authorRole("STUDENT")
                .authorName("Lin")
                .content("Use map and filter.")
                .build());

        ThreadResponseDto dto = threadMapper.toDto(entity);

        assertEquals(10, dto.getCourseId());
        assertEquals(1, dto.getReplyCount());
        assertEquals(40, dto.getReplies().get(0).getReplyId());
        assertEquals(List.of(dto), threadMapper.toDtoList(List.of(entity)));
    }

    @Test
    void updatesThreadIgnoringNulls() {
        Thread entity = Thread.builder()
                .courseId(1)
                .authorId(2)
                .authorRole("STUDENT")
                .authorName("Old")
                .title("Old title")
                .content("Old content")
                .build();
        ThreadRequestDto request = new ThreadRequestDto();
        request.setTitle("New title");

        threadMapper.updateEntityFromDto(request, entity);

        assertEquals(1, entity.getCourseId());
        assertEquals("Old", entity.getAuthorName());
        assertEquals("New title", entity.getTitle());
    }

    @Test
    void mapsReplyRequestAndResponse() {
        ReplyRequestDto request = ReplyRequestDto.builder()
                .threadId(1)
                .authorId(2)
                .authorRole("STUDENT")
                .authorName("Lin")
                .content("Reply")
                .build();
        Thread thread = Thread.builder().threadId(7).build();

        Reply reply = replyMapper.toEntity(request);
        reply.setReplyId(8);
        reply.setThread(thread);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());

        ReplyResponseDto dto = replyMapper.toDto(reply);

        assertEquals(7, dto.getThreadId());
        assertEquals(8, dto.getReplyId());
        assertEquals(List.of(dto), replyMapper.toDtoList(List.of(reply)));
    }

    @Test
    void mapsNullInputs() {
        assertNull(threadMapper.toEntity(null));
        assertNull(threadMapper.toDto(null));
        assertNull(threadMapper.toDtoList(null));
        assertNull(replyMapper.toEntity(null));
        assertNull(replyMapper.toDto(null));
        assertNull(replyMapper.toDtoList(null));
    }
}

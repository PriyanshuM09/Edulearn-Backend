package com.edulearn.discussion.service.impl;

import com.edulearn.discussion.dto.ReplyRequestDto;
import com.edulearn.discussion.dto.ReplyResponseDto;
import com.edulearn.discussion.dto.ThreadRequestDto;
import com.edulearn.discussion.dto.ThreadResponseDto;
import com.edulearn.discussion.entity.Reply;
import com.edulearn.discussion.entity.Thread;
import com.edulearn.discussion.exception.ReplyNotFoundException;
import com.edulearn.discussion.exception.ThreadNotFoundException;
import com.edulearn.discussion.mapper.ReplyMapper;
import com.edulearn.discussion.mapper.ThreadMapper;
import com.edulearn.discussion.repository.ReplyRepository;
import com.edulearn.discussion.repository.ThreadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock
    private ThreadRepository threadRepository;
    @Mock
    private ReplyRepository replyRepository;
    @Mock
    private ThreadMapper threadMapper;
    @Mock
    private ReplyMapper replyMapper;

    @InjectMocks
    private DiscussionServiceImpl discussionService;

    @Test
    @DisplayName("Test Create Thread - Success")
    void createThread_Success() {
        // given
        ThreadRequestDto request = new ThreadRequestDto();
        request.setCourseId(1);
        request.setTitle("Java Help");

        Thread thread = new Thread();
        thread.setThreadId(1);
        ThreadResponseDto response = new ThreadResponseDto();
        response.setThreadId(1);

        when(threadMapper.toEntity(any(ThreadRequestDto.class))).thenReturn(thread);
        when(threadRepository.save(any(Thread.class))).thenReturn(thread);
        when(threadMapper.toDto(any(Thread.class))).thenReturn(response);

        // when
        ThreadResponseDto result = discussionService.createThread(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.getThreadId());
        verify(threadRepository, times(1)).save(any(Thread.class));
    }

    @Test
    @DisplayName("Test Get Thread By ID - Success")
    void getThreadById_Success() {
        // given
        Integer id = 1;
        Thread thread = new Thread();
        thread.setThreadId(id);
        thread.setViewCount(0);
        ThreadResponseDto response = new ThreadResponseDto();

        when(threadRepository.findById(id)).thenReturn(Optional.of(thread));
        when(threadMapper.toDto(any(Thread.class))).thenReturn(response);

        // when
        ThreadResponseDto result = discussionService.getThreadById(id);

        // then
        assertNotNull(result);
        assertEquals(1, thread.getViewCount());
        verify(threadRepository, times(1)).incrementViewCount(id);
    }

    @Test
    @DisplayName("Test Get Threads By Course - Success")
    void getThreadsByCourse_Success() {
        // given
        Integer courseId = 1;
        when(threadRepository.findByCourseIdOrderByIsPinnedDescCreatedAtDesc(courseId)).thenReturn(List.of(new Thread()));
        when(threadMapper.toDtoList(anyList())).thenReturn(List.of(new ThreadResponseDto()));

        // when
        List<ThreadResponseDto> result = discussionService.getThreadsByCourse(courseId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Update Thread - Success")
    void updateThread_Success() {
        Integer id = 1;
        Thread thread = new Thread();
        ThreadRequestDto request = new ThreadRequestDto();
        request.setTitle("Updated");
        ThreadResponseDto response = new ThreadResponseDto();
        response.setTitle("Updated");

        when(threadRepository.findById(id)).thenReturn(Optional.of(thread));
        when(threadRepository.save(thread)).thenReturn(thread);
        when(threadMapper.toDto(thread)).thenReturn(response);

        ThreadResponseDto result = discussionService.updateThread(id, request);

        assertEquals("Updated", result.getTitle());
        verify(threadMapper).updateEntityFromDto(request, thread);
    }

    @Test
    @DisplayName("Test Delete Thread - Success")
    void deleteThread_Success() {
        when(threadRepository.existsById(1)).thenReturn(true);

        discussionService.deleteThread(1);

        verify(threadRepository).deleteById(1);
    }

    @Test
    @DisplayName("Test Delete Thread - Not Found")
    void deleteThread_NotFound() {
        when(threadRepository.existsById(1)).thenReturn(false);

        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.deleteThread(1));
        verify(threadRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("Test Pin, Unpin and Close Thread")
    void moderationActions_Success() {
        Thread thread = new Thread();
        thread.setThreadId(1);
        ThreadResponseDto response = new ThreadResponseDto();
        response.setThreadId(1);

        when(threadRepository.findById(1)).thenReturn(Optional.of(thread));
        when(threadRepository.save(thread)).thenReturn(thread);
        when(threadMapper.toDto(thread)).thenReturn(response);

        assertSame(response, discussionService.pinThread(1));
        assertTrue(thread.getIsPinned());
        assertEquals("PINNED", thread.getStatus());

        assertSame(response, discussionService.unpinThread(1));
        assertFalse(thread.getIsPinned());
        assertEquals("OPEN", thread.getStatus());

        assertSame(response, discussionService.closeThread(1));
        assertEquals("CLOSED", thread.getStatus());
    }

    @Test
    @DisplayName("Test Search and Pinned Threads")
    void searchAndPinnedThreads_Success() {
        Thread thread = new Thread();
        ThreadResponseDto response = new ThreadResponseDto();

        when(threadRepository.searchByCourseIdAndKeyword(1, "java"))
                .thenReturn(List.of(thread));
        when(threadRepository.findByCourseIdAndIsPinned(1, true))
                .thenReturn(List.of(thread));
        when(threadMapper.toDtoList(List.of(thread)))
                .thenReturn(List.of(response));

        assertEquals(1, discussionService.searchThreads(1, "java").size());
        assertEquals(1, discussionService.getPinnedThreads(1).size());
    }

    @Test
    @DisplayName("Test Add Reply - Success")
    void addReply_Success() {
        Thread thread = new Thread();
        thread.setThreadId(1);
        thread.setStatus("OPEN");
        ReplyRequestDto request = new ReplyRequestDto();
        request.setThreadId(1);
        Reply reply = new Reply();
        ReplyResponseDto response = new ReplyResponseDto();
        response.setThreadId(1);

        when(threadRepository.findById(1)).thenReturn(Optional.of(thread));
        when(replyMapper.toEntity(request)).thenReturn(reply);
        when(replyRepository.save(reply)).thenReturn(reply);
        when(replyMapper.toDto(reply)).thenReturn(response);

        ReplyResponseDto result = discussionService.addReply(request);

        assertEquals(1, result.getThreadId());
        assertSame(thread, reply.getThread());
    }

    @Test
    @DisplayName("Test Add Reply - Closed Thread")
    void addReply_ClosedThread() {
        Thread thread = new Thread();
        thread.setStatus("CLOSED");
        ReplyRequestDto request = new ReplyRequestDto();
        request.setThreadId(1);

        when(threadRepository.findById(1)).thenReturn(Optional.of(thread));

        assertThrows(RuntimeException.class,
                () -> discussionService.addReply(request));
        verify(replyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test Get Replies By Thread - Success")
    void getRepliesByThread_Success() {
        Reply reply = new Reply();
        ReplyResponseDto response = new ReplyResponseDto();

        when(threadRepository.existsById(1)).thenReturn(true);
        when(replyRepository.findByThread_ThreadIdAndIsDeletedFalse(1))
                .thenReturn(List.of(reply));
        when(replyMapper.toDtoList(List.of(reply))).thenReturn(List.of(response));

        assertEquals(1, discussionService.getRepliesByThread(1).size());
    }

    @Test
    @DisplayName("Test Update, Delete and Accept Reply")
    void replyMutations_Success() {
        Reply reply = new Reply();
        ReplyResponseDto response = new ReplyResponseDto();

        when(replyRepository.findById(1)).thenReturn(Optional.of(reply));
        when(replyRepository.save(reply)).thenReturn(reply);
        when(replyMapper.toDto(reply)).thenReturn(response);

        assertSame(response, discussionService.updateReply(1, "new content"));
        assertEquals("new content", reply.getContent());

        discussionService.deleteReply(1);
        assertTrue(reply.getIsDeleted());

        assertSame(response, discussionService.markAsAccepted(1));
        assertTrue(reply.getIsAccepted());
    }

    @Test
    @DisplayName("Test Not Found Branches")
    void notFoundBranches() {
        when(threadRepository.findById(99)).thenReturn(Optional.empty());
        when(threadRepository.existsById(99)).thenReturn(false);
        when(replyRepository.findById(99)).thenReturn(Optional.empty());
        ThreadRequestDto request = new ThreadRequestDto();

        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.getThreadById(99));
        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.updateThread(99, request));
        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.pinThread(99));
        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.unpinThread(99));
        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.closeThread(99));
        assertThrows(ThreadNotFoundException.class,
                () -> discussionService.getRepliesByThread(99));
        assertThrows(ReplyNotFoundException.class,
                () -> discussionService.updateReply(99, "content"));
        assertThrows(ReplyNotFoundException.class,
                () -> discussionService.deleteReply(99));
        assertThrows(ReplyNotFoundException.class,
                () -> discussionService.markAsAccepted(99));
    }
}

package com.edulearn.discussion.service;

import com.edulearn.discussion.dto.*;

import java.util.List;

public interface DiscussionService {

    // Thread operations
    ThreadResponseDto createThread(ThreadRequestDto requestDto);

    List<ThreadResponseDto> getThreadsByCourse(Integer courseId);

    ThreadResponseDto getThreadById(Integer threadId);

    ThreadResponseDto updateThread(Integer threadId,
                                   ThreadRequestDto requestDto);

    void deleteThread(Integer threadId);

    ThreadResponseDto pinThread(Integer threadId);

    ThreadResponseDto unpinThread(Integer threadId);

    ThreadResponseDto closeThread(Integer threadId);

    List<ThreadResponseDto> searchThreads(Integer courseId,
                                           String keyword);

    List<ThreadResponseDto> getPinnedThreads(Integer courseId);

    // Reply operations
    ReplyResponseDto addReply(ReplyRequestDto requestDto);

    List<ReplyResponseDto> getRepliesByThread(Integer threadId);

    ReplyResponseDto updateReply(Integer replyId,
                                  String content);

    void deleteReply(Integer replyId);

    ReplyResponseDto markAsAccepted(Integer replyId);
}
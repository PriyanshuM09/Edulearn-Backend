package com.edulearn.discussion.service.impl;

import com.edulearn.discussion.dto.*;
import com.edulearn.discussion.entity.Reply;
import com.edulearn.discussion.entity.Thread;
import com.edulearn.discussion.exception.*;
import com.edulearn.discussion.mapper.ReplyMapper;
import com.edulearn.discussion.mapper.ThreadMapper;
import com.edulearn.discussion.repository.ReplyRepository;
import com.edulearn.discussion.repository.ThreadRepository;
import com.edulearn.discussion.service.DiscussionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscussionServiceImpl implements DiscussionService {

    private final ThreadMapper threadMapper;
    private final ReplyMapper replyMapper;
    private final ThreadRepository threadRepository;
    private final ReplyRepository replyRepository;

    private static final String STATUS_PINNED = "PINNED";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";

    // ── THREAD OPERATIONS ─────────────────────────────────────────────────

    @Override
    @Transactional
    public ThreadResponseDto createThread(
            ThreadRequestDto requestDto) {
        log.info("Creating thread '{}' for course: {}",
                requestDto.getTitle(), requestDto.getCourseId());
        Thread thread = threadMapper.toEntity(requestDto);
        Thread saved = threadRepository.save(thread);
        log.info("Thread created with ID: {}", saved.getThreadId());
        return threadMapper.toDto(saved);
    }

    @Override
    public List<ThreadResponseDto> getThreadsByCourse(
            Integer courseId) {
        log.info("Fetching threads for course: {}", courseId);
        return threadMapper.toDtoList(
                threadRepository
                        .findByCourseIdOrderByIsPinnedDescCreatedAtDesc(
                                courseId));
    }

    @Override
    @Transactional
    public ThreadResponseDto getThreadById(Integer threadId) {
        log.info("Fetching thread ID: {}", threadId);
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ThreadNotFoundException(threadId));

        // Increment view count
        threadRepository.incrementViewCount(threadId);
        thread.setViewCount(thread.getViewCount() + 1);

        return threadMapper.toDto(thread);
    }

    @Override
    @Transactional
    public ThreadResponseDto updateThread(Integer threadId,
                                          ThreadRequestDto requestDto) {
        log.info("Updating thread ID: {}", threadId);
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ThreadNotFoundException(threadId));
        threadMapper.updateEntityFromDto(requestDto, thread);
        return threadMapper.toDto(threadRepository.save(thread));
    }

    @Override
    @Transactional
    public void deleteThread(Integer threadId) {
        log.info("Deleting thread ID: {}", threadId);
        if (!threadRepository.existsById(threadId)) {
            throw new ThreadNotFoundException(threadId);
        }
        threadRepository.deleteById(threadId);
        log.info("Thread {} deleted successfully", threadId);
    }

    @Override
    @Transactional
    public ThreadResponseDto pinThread(Integer threadId) {
        log.info("Pinning thread ID: {}", threadId);
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ThreadNotFoundException(threadId));
        thread.setIsPinned(true);
        thread.setStatus(STATUS_PINNED);
        return threadMapper.toDto(threadRepository.save(thread));
    }

    @Override
    @Transactional
    public ThreadResponseDto unpinThread(Integer threadId) {
        log.info("Unpinning thread ID: {}", threadId);
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ThreadNotFoundException(threadId));
        thread.setIsPinned(false);
        thread.setStatus(STATUS_OPEN);
        return threadMapper.toDto(threadRepository.save(thread));
    }

    @Override
    @Transactional
    public ThreadResponseDto closeThread(Integer threadId) {
        log.info("Closing thread ID: {}", threadId);
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() ->
                        new ThreadNotFoundException(threadId));
        thread.setStatus(STATUS_CLOSED);
        return threadMapper.toDto(threadRepository.save(thread));
    }

    @Override
    public List<ThreadResponseDto> searchThreads(
            Integer courseId, String keyword) {
        log.info("Searching threads in course: {} keyword: {}",
                courseId, keyword);
        return threadMapper.toDtoList(
                threadRepository.searchByCourseIdAndKeyword(
                        courseId, keyword));
    }

    @Override
    public List<ThreadResponseDto> getPinnedThreads(
            Integer courseId) {
        log.info("Fetching pinned threads for course: {}",
                courseId);
        return threadMapper.toDtoList(
                threadRepository.findByCourseIdAndIsPinned(
                        courseId, true));
    }

    // ── REPLY OPERATIONS ──────────────────────────────────────────────────

    @Override
    @Transactional
    public ReplyResponseDto addReply(ReplyRequestDto requestDto) {
        log.info("Adding reply to thread ID: {}",
                requestDto.getThreadId());

        Thread thread = threadRepository
                .findById(requestDto.getThreadId())
                .orElseThrow(() -> new ThreadNotFoundException(
                        requestDto.getThreadId()));

        if (STATUS_CLOSED.equals(thread.getStatus())) {
            throw new RuntimeException(
                    "Cannot reply to a closed thread");
        }

        Reply reply = replyMapper.toEntity(requestDto);
        reply.setThread(thread);
        Reply saved = replyRepository.save(reply);

        log.info("Reply created with ID: {}", saved.getReplyId());
        return replyMapper.toDto(saved);
    }

    @Override
    public List<ReplyResponseDto> getRepliesByThread(
            Integer threadId) {
        log.info("Fetching replies for thread ID: {}", threadId);
        if (!threadRepository.existsById(threadId)) {
            throw new ThreadNotFoundException(threadId);
        }
        return replyMapper.toDtoList(
                replyRepository
                        .findByThread_ThreadIdAndIsDeletedFalse(
                                threadId));
    }

    @Override
    @Transactional
    public ReplyResponseDto updateReply(Integer replyId,
                                         String content) {
        log.info("Updating reply ID: {}", replyId);
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() ->
                        new ReplyNotFoundException(replyId));
        reply.setContent(content);
        return replyMapper.toDto(replyRepository.save(reply));
    }

    @Override
    @Transactional
    public void deleteReply(Integer replyId) {
        log.info("Soft deleting reply ID: {}", replyId);
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() ->
                        new ReplyNotFoundException(replyId));
        // Soft delete — keeps record but hides from view
        reply.setIsDeleted(true);
        replyRepository.save(reply);
        log.info("Reply {} soft deleted", replyId);
    }

    @Override
    @Transactional
    public ReplyResponseDto markAsAccepted(Integer replyId) {
        log.info("Marking reply ID: {} as accepted", replyId);
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() ->
                        new ReplyNotFoundException(replyId));
        reply.setIsAccepted(true);
        return replyMapper.toDto(replyRepository.save(reply));
    }
}
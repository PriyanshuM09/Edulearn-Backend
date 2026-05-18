package com.edulearn.discussion.resource;

import com.edulearn.discussion.dto.*;
import com.edulearn.discussion.service.DiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/discussions")
@RequiredArgsConstructor
@Tag(name = "Discussion API",
     description = "Course forums, threads, replies and moderation")
public class DiscussionResource {

    private final DiscussionService discussionService;

    // ── THREAD ENDPOINTS ───────────────────────────────────────────────────

    @PostMapping("/threads")
    @Operation(summary = "Create a new discussion thread")
    public ResponseEntity<ThreadResponseDto> createThread(
            @Valid @RequestBody ThreadRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.createThread(requestDto));
    }

    @GetMapping("/threads/course/{courseId}")
    @Operation(summary = "Get all threads for a course — pinned first")
    public ResponseEntity<List<ThreadResponseDto>> getThreadsByCourse(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(
                discussionService.getThreadsByCourse(courseId));
    }

    @GetMapping("/threads/{threadId}")
    @Operation(summary = "Get thread by ID — also increments view count")
    public ResponseEntity<ThreadResponseDto> getThreadById(
            @PathVariable Integer threadId) {
        return ResponseEntity.ok(
                discussionService.getThreadById(threadId));
    }

    @PutMapping("/threads/{threadId}")
    @Operation(summary = "Update a thread")
    public ResponseEntity<ThreadResponseDto> updateThread(
            @PathVariable Integer threadId,
            @Valid @RequestBody ThreadRequestDto requestDto) {
        return ResponseEntity.ok(
                discussionService.updateThread(
                        threadId, requestDto));
    }

    @DeleteMapping("/threads/{threadId}")
    @Operation(summary = "Delete a thread and all its replies")
    public ResponseEntity<Void> deleteThread(
            @PathVariable Integer threadId) {
        discussionService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }

    // ── MODERATION ENDPOINTS ───────────────────────────────────────────────

    @PutMapping("/threads/{threadId}/pin")
    @Operation(summary = "Pin a thread — moderation")
    public ResponseEntity<ThreadResponseDto> pinThread(
            @PathVariable Integer threadId) {
        return ResponseEntity.ok(
                discussionService.pinThread(threadId));
    }

    @PutMapping("/threads/{threadId}/unpin")
    @Operation(summary = "Unpin a thread — moderation")
    public ResponseEntity<ThreadResponseDto> unpinThread(
            @PathVariable Integer threadId) {
        return ResponseEntity.ok(
                discussionService.unpinThread(threadId));
    }

    @PutMapping("/threads/{threadId}/close")
    @Operation(summary = "Close a thread — no more replies allowed")
    public ResponseEntity<ThreadResponseDto> closeThread(
            @PathVariable Integer threadId) {
        return ResponseEntity.ok(
                discussionService.closeThread(threadId));
    }

    @GetMapping("/threads/course/{courseId}/pinned")
    @Operation(summary = "Get all pinned threads for a course")
    public ResponseEntity<List<ThreadResponseDto>> getPinnedThreads(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(
                discussionService.getPinnedThreads(courseId));
    }

    @GetMapping("/threads/course/{courseId}/search")
    @Operation(summary = "Search threads by keyword in title or content")
    public ResponseEntity<List<ThreadResponseDto>> searchThreads(
            @PathVariable Integer courseId,
            @RequestParam String keyword) {
        return ResponseEntity.ok(
                discussionService.searchThreads(
                        courseId, keyword));
    }

    // ── REPLY ENDPOINTS ────────────────────────────────────────────────────

    @PostMapping("/replies")
    @Operation(summary = "Add a reply to a thread")
    public ResponseEntity<ReplyResponseDto> addReply(
            @Valid @RequestBody ReplyRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionService.addReply(requestDto));
    }

    @GetMapping("/replies/thread/{threadId}")
    @Operation(summary = "Get all replies for a thread")
    public ResponseEntity<List<ReplyResponseDto>> getRepliesByThread(
            @PathVariable Integer threadId) {
        return ResponseEntity.ok(
                discussionService.getRepliesByThread(threadId));
    }

    @PutMapping("/replies/{replyId}")
    @Operation(summary = "Update reply content")
    public ResponseEntity<ReplyResponseDto> updateReply(
            @PathVariable Integer replyId,
            @RequestParam String content) {
        return ResponseEntity.ok(
                discussionService.updateReply(replyId, content));
    }

    @DeleteMapping("/replies/{replyId}")
    @Operation(summary = "Soft delete a reply")
    public ResponseEntity<Void> deleteReply(
            @PathVariable Integer replyId) {
        discussionService.deleteReply(replyId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/replies/{replyId}/accept")
    @Operation(summary = "Mark reply as accepted best answer")
    public ResponseEntity<ReplyResponseDto> markAsAccepted(
            @PathVariable Integer replyId) {
        return ResponseEntity.ok(
                discussionService.markAsAccepted(replyId));
    }
}
package com.edulearn.discussion.repository;

import com.edulearn.discussion.entity.Thread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadRepository
        extends JpaRepository<Thread, Integer> {

    List<Thread> findByCourseIdOrderByIsPinnedDescCreatedAtDesc(
            Integer courseId);

    List<Thread> findByCourseIdAndStatus(
            Integer courseId, String status);

    List<Thread> findByAuthorId(Integer authorId);

    List<Thread> findByCourseIdAndIsPinned(
            Integer courseId, Boolean isPinned);

    @Query("SELECT t FROM Thread t WHERE t.courseId = :courseId " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Thread> searchByCourseIdAndKeyword(
            @Param("courseId") Integer courseId,
            @Param("keyword") String keyword);

    @Modifying
    @Query("UPDATE Thread t SET t.viewCount = t.viewCount + 1 " +
           "WHERE t.threadId = :threadId")
    void incrementViewCount(@Param("threadId") Integer threadId);

    long countByCourseId(Integer courseId);
}
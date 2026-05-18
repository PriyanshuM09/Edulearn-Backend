package com.edulearn.discussion.repository;

import com.edulearn.discussion.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplyRepository
        extends JpaRepository<Reply, Integer> {

    List<Reply> findByThread_ThreadIdAndIsDeletedFalse(
            Integer threadId);

    List<Reply> findByAuthorId(Integer authorId);

    long countByThread_ThreadIdAndIsDeletedFalse(
            Integer threadId);

    List<Reply> findByThread_ThreadIdAndIsAccepted(
            Integer threadId, Boolean isAccepted);
}
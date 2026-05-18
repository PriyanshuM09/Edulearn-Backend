package com.edulearn.discussion.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "replies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer replyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Thread thread;

    @Column(nullable = false)
    private Integer authorId;

    @Column(nullable = false, length = 50)
    private String authorRole;          // STUDENT, INSTRUCTOR

    @Column(nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isAccepted = false; // marked as best answer

    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;  // soft delete

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
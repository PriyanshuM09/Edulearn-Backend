package com.edulearn.lesson.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Lesson lesson;

    @Column(nullable = false, length = 200)
    private String name;

    private String fileUrl;

    @Column(length = 50)
    private String fileType;            // PDF, SLIDE, CODE, etc.

    private Long sizeKb;

    @Override
    public String toString() {
        return "Resource{resourceId=" + resourceId + ", name='" + name + "'}";
    }
}
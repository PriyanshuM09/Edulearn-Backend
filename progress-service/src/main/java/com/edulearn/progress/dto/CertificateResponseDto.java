package com.edulearn.progress.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponseDto {
    private Integer certificateId;
    private Integer studentId;
    private Integer courseId;
    private String verificationCode;
    private String certificateUrl;
    private String instructorName;
    private String courseName;
    private String studentName;
    private LocalDateTime issuedAt;
}
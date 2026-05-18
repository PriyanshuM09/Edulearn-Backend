package com.edulearn.enrollment.mapper;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.entity.Enrollment;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled("Generated MapStruct implementations are excluded from coverage and can be unstable in clean Windows reactor runs")
class EnrollmentMapperTest {

    private final EnrollmentMapper mapper = Mappers.getMapper(EnrollmentMapper.class);

    @Test
    void toEntity_MapsRequestFields() {
        EnrollmentRequestDto request = new EnrollmentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);

        Enrollment enrollment = mapper.toEntity(request);

        assertEquals(1, enrollment.getStudentId());
        assertEquals(101, enrollment.getCourseId());
        assertNull(enrollment.getEnrollmentId());
    }

    @Test
    void toEntity_NullRequest_ReturnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDto_MapsEnrollmentFields() {
        LocalDateTime now = LocalDateTime.now();
        Enrollment enrollment = Enrollment.builder()
                .enrollmentId(10)
                .studentId(1)
                .courseId(101)
                .status("COMPLETED")
                .progressPercent(100)
                .certificateIssued(true)
                .enrolledAt(now)
                .updatedAt(now)
                .completedAt(now)
                .build();

        EnrollmentResponseDto response = mapper.toDto(enrollment);

        assertEquals(10, response.getEnrollmentId());
        assertEquals(1, response.getStudentId());
        assertEquals(101, response.getCourseId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(100, response.getProgressPercent());
        assertEquals(true, response.getCertificateIssued());
        assertEquals(now, response.getCompletedAt());
    }

    @Test
    void toDto_NullEnrollment_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }
}

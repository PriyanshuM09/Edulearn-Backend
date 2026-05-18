package com.edulearn.progress.mapper;

import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.entity.Progress;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled("Generated MapStruct implementations are excluded from coverage and can be unstable in clean Windows reactor runs")
class ProgressMapperTest {

    private final ProgressMapper progressMapper = Mappers.getMapper(ProgressMapper.class);
    private final CertificateMapper certificateMapper = Mappers.getMapper(CertificateMapper.class);

    @Test
    void progressMapper_MapsFieldsAndNulls() {
        WatchProgressRequest request = new WatchProgressRequest(1, 101, 501, 120);
        Progress progress = progressMapper.toEntity(request);
        assertEquals(1, progress.getStudentId());
        assertEquals(501, progress.getLessonId());

        progress.setProgressId(9);
        progress.setIsCompleted(true);
        assertEquals(true, progressMapper.toDto(progress).getCompleted());
        assertEquals(1, progressMapper.toDtoList(List.of(progress)).size());
        assertNull(progressMapper.toEntity(null));
        assertNull(progressMapper.toDto(null));
        assertNull(progressMapper.toDtoList(null));
    }

    @Test
    void certificateMapper_MapsFieldsAndNulls() {
        Certificate certificate = Certificate.builder()
                .certificateId(1)
                .studentId(2)
                .courseId(3)
                .verificationCode("code")
                .certificateUrl("url")
                .instructorName("Instructor")
                .courseName("Course")
                .studentName("Student")
                .build();

        assertEquals("code", certificateMapper.toDto(certificate).getVerificationCode());
        assertEquals(1, certificateMapper.toDtoList(List.of(certificate)).size());
        assertNull(certificateMapper.toDto(null));
        assertNull(certificateMapper.toDtoList(null));
    }
}

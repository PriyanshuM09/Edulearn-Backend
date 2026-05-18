package com.edulearn.progress.service.impl;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.dto.external.CourseDto;
import com.edulearn.progress.dto.external.UserProfileDto;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.mapper.CertificateMapper;
import com.edulearn.progress.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceImplTest {

    @Mock
    private CertificateRepository certificateRepository;
    @Mock
    private CertificateMapper certificateMapper;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RestTemplate directRestTemplate;

    private CertificateServiceImpl certificateService;
    private Path uploadDir;

    @BeforeEach
    void setUp() throws Exception {
        certificateService = new CertificateServiceImpl(
                certificateRepository, certificateMapper, restTemplate, directRestTemplate);
        uploadDir = Path.of("target", "test-certificates").toAbsolutePath();
        Files.createDirectories(uploadDir);
        ReflectionTestUtils.setField(certificateService, "uploadDir", uploadDir.toString());
        ReflectionTestUtils.setField(certificateService, "baseUrl", "http://files");
        ReflectionTestUtils.setField(certificateService, "authBaseUrl", "http://auth");
        ReflectionTestUtils.setField(certificateService, "courseBaseUrl", "http://course");
    }

    @Test
    void generateCertificate_NewCertificate_UsesResolvedNames() {
        UserProfileDto student = new UserProfileDto();
        student.setFullName("Ada Student");
        CourseDto course = new CourseDto();
        course.setTitle("Java");
        course.setInstructorName("Grace Instructor");

        when(certificateRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());
        when(directRestTemplate.getForObject(contains("/profile/1"), eq(UserProfileDto.class))).thenReturn(student);
        when(directRestTemplate.getForObject(contains("/courses/101"), eq(CourseDto.class))).thenReturn(course);
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Certificate result = certificateService.generateCertificate(1, 101);

        assertEquals("Ada Student", result.getStudentName());
        assertEquals("Java", result.getCourseName());
        assertEquals("Grace Instructor", result.getInstructorName());
        assertTrue(result.getCertificateUrl().startsWith("http://files/certificates/"));
    }

    @Test
    void generateCertificate_ExistingFileWithRealNames_ReturnsExisting() throws Exception {
        Certificate existing = Certificate.builder()
                .studentId(1)
                .courseId(101)
                .verificationCode("existing")
                .studentName("Ada Student")
                .instructorName("Grace Instructor")
                .courseName("Java")
                .build();
        Path certificatePath = uploadDir.resolve("certificates").resolve("existing.pdf");
        Files.createDirectories(certificatePath.getParent());
        Files.write(certificatePath, new byte[] {1, 2, 3});
        when(certificateRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.of(existing));

        assertSame(existing, certificateService.generateCertificate(1, 101));

        verify(certificateRepository, never()).save(any());
    }

    @Test
    void generateCertificate_FallsBackWhenExternalLookupsFail() {
        when(certificateRepository.findByStudentIdAndCourseId(1, 101)).thenReturn(Optional.empty());
        when(directRestTemplate.getForObject(anyString(), any(Class.class)))
                .thenThrow(new RuntimeException("down"));
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Certificate result = certificateService.generateCertificate(1, 101);

        assertEquals("Student", result.getStudentName());
        assertEquals("Course #101", result.getCourseName());
        assertEquals("EduLearn Instructor", result.getInstructorName());
    }

    @Test
    void certificateReadsAndDownload_Success() throws Exception {
        Certificate certificate = Certificate.builder()
                .certificateId(1)
                .studentId(1)
                .courseId(101)
                .verificationCode("download")
                .build();
        Path certificatePath = uploadDir.resolve("certificates").resolve("download.pdf");
        Files.createDirectories(certificatePath.getParent());
        Files.write(certificatePath, new byte[] {4, 5, 6});

        when(certificateRepository.findByStudentId(1)).thenReturn(List.of(certificate));
        when(certificateRepository.findByVerificationCode("download")).thenReturn(Optional.of(certificate));
        when(certificateRepository.findById(1)).thenReturn(Optional.of(certificate));
        when(certificateMapper.toDto(certificate)).thenReturn(new CertificateResponseDto());

        assertEquals(1, certificateService.getStudentCertificates(1).size());
        assertNotNull(certificateService.verifyCertificate("download"));
        assertArrayEquals(new byte[] {4, 5, 6}, certificateService.downloadCertificate(1));
    }

    @Test
    void verifyCertificate_Missing_Throws() {
        when(certificateRepository.findByVerificationCode("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> certificateService.verifyCertificate("missing"));
    }
}

package com.edulearn.progress.service.impl;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.dto.external.CourseDto;
import com.edulearn.progress.dto.external.UserProfileDto;
import com.edulearn.progress.entity.Certificate;
import com.edulearn.progress.mapper.CertificateMapper;
import com.edulearn.progress.repository.CertificateRepository;
import com.edulearn.progress.service.CertificateService;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final RestTemplate restTemplate; // Load-balanced (for general use)

    @Qualifier("directRestTemplate")
    private final RestTemplate directRestTemplate; // Direct (for reliable lookup)

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.base.url}")
    private String baseUrl;

    @Value("${services.auth.base-url:http://localhost:8081}")
    private String authBaseUrl;

    @Value("${services.course.base-url:http://localhost:8082}")
    private String courseBaseUrl;

    @Override
    @Transactional
    public Certificate generateCertificate(Integer studentId, Integer courseId) {
        Certificate certificate = certificateRepository.findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        
        if (certificate != null) {
            String filePath = uploadDir + "/certificates/" + certificate.getVerificationCode() + ".pdf";
            boolean fileExists = Files.exists(Paths.get(filePath));

            // Detect if certificate was previously generated with raw IDs, "Student", or placeholder names
            boolean isPlaceholderStudent = certificate.getStudentName() != null
                    && (certificate.getStudentName().matches("\\d+") || 
                        certificate.getStudentName().startsWith("Student #") || 
                        "Student".equals(certificate.getStudentName()));
            
            boolean isPlaceholderInstructor = certificate.getInstructorName() != null
                    && (certificate.getInstructorName().matches("\\d+") || 
                        certificate.getInstructorName().contains("Instructor #") || 
                        "EduLearn Instructor".equals(certificate.getInstructorName()));

            if (fileExists && !isPlaceholderStudent && !isPlaceholderInstructor) {
                log.info("Certificate already exists on disk for student {} and course {}", studentId, courseId);
                return certificate;
            }
            if (isPlaceholderStudent || isPlaceholderInstructor) {
                log.warn("Certificate for student {} has placeholder names (student='{}', instructor='{}'). Force-regenerating...",
                        studentId, certificate.getStudentName(), certificate.getInstructorName());
            } else {
                log.warn("Certificate record exists but file is missing. Re-generating file...");
            }
        }

        log.info("Generating/Refreshing certificate for Student {} and Course {}", studentId, courseId);

        try {
            // Fetch student info with fallback
            UserProfileDto studentProfile = null;
            try {
                // Use direct URL to bypass Eureka flakiness
                studentProfile = directRestTemplate.getForObject(authBaseUrl + "/api/v1/auth/profile/" + studentId, UserProfileDto.class);
            } catch (Exception e) {
                log.warn("Could not fetch profile for student {} via direct URL {}: {}", studentId, authBaseUrl, e.getMessage());
            }

            // Fetch course info with fallback
            CourseDto courseDto = null;
            try {
                // Use direct URL to bypass Eureka flakiness
                courseDto = directRestTemplate.getForObject(courseBaseUrl + "/api/v1/courses/" + courseId, CourseDto.class);
            } catch (Exception e) {
                log.warn("Could not fetch details for course {} via direct URL {}: {}", courseId, courseBaseUrl, e.getMessage());
            }

            // ── Resolve Student Name ───────────────────────────────────────────
            String sName = "Student";
            if (studentProfile != null && studentProfile.getFullName() != null && !studentProfile.getFullName().isBlank()) {
                sName = studentProfile.getFullName();
                log.info("Resolved student name: '{}' for studentId {}", sName, studentId);
            } else {
                log.warn("Could not resolve student name for studentId {}. Profile: {}", studentId, studentProfile);
            }

            // ── Resolve Course Title ───────────────────────────────────────────
            String cTitle = (courseDto != null && courseDto.getTitle() != null) ? courseDto.getTitle() : "Course #" + courseId;

            // ── Resolve Instructor Name ────────────────────────────────────────
            // First, try what the course-service already resolved
            String iName = null;
            if (courseDto != null && courseDto.getInstructorName() != null && !courseDto.getInstructorName().isBlank()) {
                iName = courseDto.getInstructorName();
                log.info("Resolved instructor name from course-service: '{}'", iName);
            }

            // Fallback: if course-service didn't populate it, fetch instructor directly from auth-service
            if (iName == null && courseDto != null && courseDto.getInstructorId() != null) {
                log.info("instructorName was null from course-service; fetching instructor profile directly for instructorId {}", courseDto.getInstructorId());
                try {
                    // Use direct URL to bypass Eureka flakiness
                    UserProfileDto instructorProfile = directRestTemplate.getForObject(
                            authBaseUrl + "/api/v1/auth/profile/" + courseDto.getInstructorId(),
                            UserProfileDto.class);
                    if (instructorProfile != null && instructorProfile.getFullName() != null && !instructorProfile.getFullName().isBlank()) {
                        iName = instructorProfile.getFullName();
                        log.info("Resolved instructor name from auth-service directly: '{}'", iName);
                    }
                } catch (Exception ex) {
                    log.warn("Could not fetch instructor profile for instructorId {}: {}", courseDto.getInstructorId(), ex.getMessage());
                }
            }

            if (iName == null) {
                iName = "EduLearn Instructor";
                log.warn("Could not resolve instructor name; using fallback.");
            }

            // ── Build file path ────────────────────────────────────────────────
            String verificationCode = (certificate != null) ? certificate.getVerificationCode() : UUID.randomUUID().toString();
            String fileName = verificationCode + ".pdf";
            String subDir = "/certificates/";
            String fullDirPath = uploadDir + subDir;
            String filePath = fullDirPath + fileName;

            Files.createDirectories(Paths.get(fullDirPath));

            createPdf(filePath, sName, cTitle, iName, verificationCode);

            if (certificate == null) {
                certificate = Certificate.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .verificationCode(verificationCode)
                        .studentName(sName)
                        .courseName(cTitle)
                        .instructorName(iName)
                        .certificateUrl(baseUrl + subDir + fileName)
                        .build();
                return certificateRepository.save(certificate);
            } else {
                certificate.setStudentName(sName);
                certificate.setCourseName(cTitle);
                certificate.setInstructorName(iName);
                return certificateRepository.save(certificate);
            }
        } catch (Exception e) {
            log.error("CRITICAL: Certificate generation failed", e);
            throw new RuntimeException("Certificate generation failed: " + e.getMessage());
        }
    }

    private void createPdf(String dest, String studentName, String courseName, String instructorName, String verificationCode) throws Exception {
        PdfWriter writer = new PdfWriter(new FileOutputStream(dest));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(50, 50, 50, 50);

        DeviceRgb mainBlue = new DeviceRgb(0, 51, 102);

        document.add(new Paragraph("EduLearn LMS")
                .setFontColor(mainBlue)
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("\n\n"));

        document.add(new Paragraph("Certificate of Completion")
                .setFontSize(36)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("This is to certify that")
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(studentName)
                .setFontSize(30)
                .setBold()
                .setFontColor(mainBlue)
                .setTextAlignment(TextAlignment.CENTER)
                .setUnderline());

        document.add(new Paragraph("has successfully completed the course")
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(courseName)
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("\n"));

        Table table = new Table(2);
        table.setWidth(500);
        table.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);

        table.addCell(new Cell().add(new Paragraph("Instructor: " + (instructorName != null ? instructorName : "EduLearn Team")))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT));

        table.addCell(new Cell().add(new Paragraph("Date: " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT));

        document.add(table);

        document.add(new Paragraph("\n\n\n"));

        document.add(new Paragraph("Verification Code: " + verificationCode)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.close();
    }

    @Override
    public List<CertificateResponseDto> getStudentCertificates(Integer studentId) {
        return certificateRepository.findByStudentId(studentId).stream()
                .map(certificateMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CertificateResponseDto verifyCertificate(String verificationCode) {
        return certificateRepository.findByVerificationCode(verificationCode)
                .map(certificateMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
    }

    @Override
    public byte[] downloadCertificate(Integer certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));
        
        String fileName = certificate.getVerificationCode() + ".pdf";
        java.nio.file.Path path = java.nio.file.Paths.get(uploadDir + "/certificates/" + fileName);
        
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            log.error("Error reading certificate file", e);
            throw new RuntimeException("Could not read certificate file");
        }
    }
}

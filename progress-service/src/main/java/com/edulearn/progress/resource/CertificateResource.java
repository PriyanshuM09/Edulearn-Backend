package com.edulearn.progress.resource;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/progress/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificates", description = "Endpoints for certificate management and verification")
public class CertificateResource {

    private final CertificateService certificateService;

    @Operation(summary = "Get all certificates for a student")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CertificateResponseDto>> getStudentCertificates(@PathVariable Integer studentId) {
        return ResponseEntity.ok(certificateService.getStudentCertificates(studentId));
    }

    @Operation(summary = "Verify certificate authenticity by code")
    @GetMapping("/verify/{verificationCode}")
    public ResponseEntity<CertificateResponseDto> verifyCertificate(@PathVariable String verificationCode) {
        return ResponseEntity.ok(certificateService.verifyCertificate(verificationCode));
    }

    @Operation(summary = "Download certificate PDF")
    @GetMapping("/{certificateId}/download")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Integer certificateId) {
        byte[] pdfContent = certificateService.downloadCertificate(certificateId);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-" + certificateId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
}

package com.edulearn.progress.service;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.entity.Certificate;
import java.util.List;

public interface CertificateService {
    Certificate generateCertificate(Integer studentId, Integer courseId);
    List<CertificateResponseDto> getStudentCertificates(Integer studentId);
    CertificateResponseDto verifyCertificate(String verificationCode);
    byte[] downloadCertificate(Integer certificateId);
}

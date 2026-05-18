package com.edulearn.progress.resource;

import com.edulearn.progress.dto.CertificateResponseDto;
import com.edulearn.progress.service.CertificateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CertificateResource.class)
@AutoConfigureMockMvc(addFilters = false)
class CertificateResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CertificateService certificateService;

    @Test
    @DisplayName("Certificate endpoints - Success")
    void certificateEndpoints_Success() throws Exception {
        CertificateResponseDto certificate = CertificateResponseDto.builder()
                .certificateId(1)
                .verificationCode("code")
                .build();
        when(certificateService.getStudentCertificates(1)).thenReturn(List.of(certificate));
        when(certificateService.verifyCertificate(anyString())).thenReturn(certificate);
        when(certificateService.downloadCertificate(1)).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/v1/progress/certificates/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verificationCode").value("code"));
        mockMvc.perform(get("/api/v1/progress/certificates/verify/code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationCode").value("code"));
        mockMvc.perform(get("/api/v1/progress/certificates/1/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"certificate-1.pdf\""));
    }
}

package com.edulearn.progress.exception;

public class CertificateNotFoundException extends RuntimeException {

    public CertificateNotFoundException(Integer certificateId) {
        super("Certificate not found with id: " + certificateId);
    }

    public CertificateNotFoundException(String message) {
        super(message);
    }
}
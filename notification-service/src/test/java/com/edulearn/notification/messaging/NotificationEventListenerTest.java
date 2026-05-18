package com.edulearn.notification.messaging;

import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private RestTemplate restTemplate;

    @Test
    void handleEnrollmentEvent_ActiveBuildsSuccessNotification() {
        NotificationEventListener listener = new NotificationEventListener(notificationService, restTemplate);

        listener.handleEnrollmentEvent(Map.of(
                "status", "ACTIVE",
                "studentId", 1,
                "courseId", 101));

        NotificationRequestDto request = capturedRequest();
        assertEquals("ENROLLMENT_SUCCESS", request.getType());
        assertEquals(1, request.getRecipientId());
        assertEquals(101, request.getReferenceId());
    }

    @Test
    void handleEnrollmentEvent_CancelledBuildsCancelledNotification() {
        NotificationEventListener listener = new NotificationEventListener(notificationService, restTemplate);

        listener.handleEnrollmentEvent(Map.of(
                "status", "CANCELLED",
                "studentId", 1,
                "courseId", 101));

        assertEquals("ENROLLMENT_CANCELLED", capturedRequest().getType());
    }

    @Test
    void handlePaymentEvent_SuccessWithIntegerAmountBuildsPaymentNotification() {
        NotificationEventListener listener = new NotificationEventListener(notificationService, restTemplate);

        listener.handlePaymentEvent(Map.of(
                "status", "SUCCESS",
                "studentId", 1,
                "courseId", 101,
                "amount", 500));

        NotificationRequestDto request = capturedRequest();
        assertEquals("PAYMENT_SUCCESS", request.getType());
        assertEquals("Payment Successful", request.getTitle());
    }

    @Test
    void handlePaymentEvent_RefundedWithStringAmountBuildsRefundNotification() {
        NotificationEventListener listener = new NotificationEventListener(notificationService, restTemplate);

        listener.handlePaymentEvent(Map.of(
                "status", "REFUNDED",
                "studentId", 1,
                "courseId", 101,
                "amount", "250.5"));

        assertEquals("PAYMENT_REFUNDED", capturedRequest().getType());
    }

    @Test
    void handlePaymentEvent_WithDoubleAmountBuildsPaymentNotification() {
        NotificationEventListener listener = new NotificationEventListener(notificationService, restTemplate);

        listener.handlePaymentEvent(Map.of(
                "status", "SUCCESS",
                "studentId", 1,
                "courseId", 101,
                "amount", 99.5d));

        verify(notificationService, times(1)).sendNotification(org.mockito.ArgumentMatchers.any());
    }

    private NotificationRequestDto capturedRequest() {
        ArgumentCaptor<NotificationRequestDto> captor =
                ArgumentCaptor.forClass(NotificationRequestDto.class);
        verify(notificationService).sendNotification(captor.capture());
        return captor.getValue();
    }
}

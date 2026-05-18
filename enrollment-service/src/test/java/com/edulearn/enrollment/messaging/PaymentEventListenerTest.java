package com.edulearn.enrollment.messaging;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.event.PaymentEvent;
import com.edulearn.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @Test
    void handlePaymentSuccess_WithSuccessStatus_EnrollsStudent() {
        PaymentEventListener listener = new PaymentEventListener(enrollmentService);
        PaymentEvent event = PaymentEvent.builder()
                .studentId(1)
                .courseId(101)
                .status("SUCCESS")
                .build();

        listener.handlePaymentSuccess(event);

        ArgumentCaptor<EnrollmentRequestDto> captor =
                ArgumentCaptor.forClass(EnrollmentRequestDto.class);
        verify(enrollmentService).enroll(captor.capture());
        assertEquals(1, captor.getValue().getStudentId());
        assertEquals(101, captor.getValue().getCourseId());
    }

    @Test
    void handlePaymentSuccess_WithNonSuccessStatus_DoesNotEnroll() {
        PaymentEventListener listener = new PaymentEventListener(enrollmentService);
        PaymentEvent event = PaymentEvent.builder()
                .studentId(1)
                .courseId(101)
                .status("FAILED")
                .build();

        listener.handlePaymentSuccess(event);

        verify(enrollmentService, never()).enroll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handlePaymentSuccess_WhenEnrollFails_SwallowsException() {
        PaymentEventListener listener = new PaymentEventListener(enrollmentService);
        PaymentEvent event = PaymentEvent.builder()
                .studentId(1)
                .courseId(101)
                .status("SUCCESS")
                .build();
        doThrow(new RuntimeException("duplicate"))
                .when(enrollmentService).enroll(org.mockito.ArgumentMatchers.any());

        listener.handlePaymentSuccess(event);

        verify(enrollmentService).enroll(org.mockito.ArgumentMatchers.any());
    }
}

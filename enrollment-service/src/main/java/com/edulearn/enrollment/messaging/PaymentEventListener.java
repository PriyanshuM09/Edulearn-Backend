package com.edulearn.enrollment.messaging;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.event.PaymentEvent;
import com.edulearn.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final EnrollmentService enrollmentService;

    @RabbitListener(queues = "edulearn.payment.enrollment.queue")
    public void handlePaymentSuccess(PaymentEvent event) {
        log.info("Received payment success event for student {} and course {}", 
                event.getStudentId(), event.getCourseId());
        
        try {
            if ("SUCCESS".equals(event.getStatus())) {
                EnrollmentRequestDto request = new EnrollmentRequestDto();
                request.setStudentId(event.getStudentId());
                request.setCourseId(event.getCourseId());
                
                enrollmentService.enroll(request);
                log.info("Successfully enrolled student {} in course {}", 
                        event.getStudentId(), event.getCourseId());
            }
        } catch (Exception e) {
            log.error("Failed to enroll student {} after payment: {}", 
                    event.getStudentId(), e.getMessage());
        }
    }
}

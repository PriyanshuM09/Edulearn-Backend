package com.edulearn.notification.messaging;

import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.dto.external.CourseDto;
import com.edulearn.notification.dto.external.UserProfileDto;
import com.edulearn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final RestTemplate restTemplate;

    @Value("${services.auth.base-url:http://auth-service:8081}")
    private String authBaseUrl;

    @Value("${services.course.base-url:http://course-service:8082}")
    private String courseBaseUrl;

    @RabbitListener(queues = "edulearn.enrollment.queue")
    public void handleEnrollmentEvent(Map<String, Object> event) {
        log.info("Received enrollment event: {}", event);
        
        String status = (String) event.get("status");
        Integer studentId = (Integer) event.get("studentId");
        Integer courseId = (Integer) event.get("courseId");
        
        // Fetch Details
        UserProfileDto student = fetchStudent(studentId);
        CourseDto course = fetchCourse(courseId);
        
        String courseName = (course != null) ? course.getTitle() : "Course #" + courseId;

        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(studentId);
        request.setRecipientEmail(student != null ? student.getEmail() : null);
        request.setRecipientRole("STUDENT");
        request.setChannel("BOTH");
        request.setReferenceId(courseId);
        request.setReferenceType("COURSE");

        if ("ACTIVE".equals(status)) {
            request.setType("ENROLLMENT_SUCCESS");
            request.setTitle("Course Enrollment Successful");
            request.setMessage("Hi " + (student != null ? student.getFullName() : "Student") + 
                ", You have successfully enrolled in: " + courseName);
        } else if ("CANCELLED".equals(status)) {
            request.setType("ENROLLMENT_CANCELLED");
            request.setTitle("Course Enrollment Cancelled");
            request.setMessage("Your enrollment for " + courseName + " has been cancelled.");
        }
        
        notificationService.sendNotification(request);
    }

    @RabbitListener(queues = "edulearn.payment.queue")
    public void handlePaymentEvent(Map<String, Object> event) {
        log.info("Received payment event: {}", event);
        
        String status = (String) event.get("status");
        Integer studentId = (Integer) event.get("studentId");
        Integer courseId = (Integer) event.get("courseId");
        Object rawAmount = event.get("amount");
        Double amount = null;
        if (rawAmount != null) {
            if (rawAmount instanceof Integer) {
                amount = ((Integer) rawAmount).doubleValue();
            } else if (rawAmount instanceof Double) {
                amount = (Double) rawAmount;
            } else {
                amount = Double.valueOf(rawAmount.toString());
            }
        }

        // Fetch Details
        UserProfileDto student = fetchStudent(studentId);
        CourseDto course = fetchCourse(courseId);
        
        String courseName = (course != null) ? course.getTitle() : "Course #" + courseId;

        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(studentId);
        request.setRecipientEmail(student != null ? student.getEmail() : null);
        request.setRecipientRole("STUDENT");
        request.setChannel("BOTH");
        request.setReferenceId(courseId);
        request.setReferenceType("COURSE");

        if ("SUCCESS".equals(status)) {
            request.setType("PAYMENT_SUCCESS");
            request.setTitle("Payment Successful");
            request.setMessage("Hi " + (student != null ? student.getFullName() : "Student") + 
                ", We have received your payment of ₹" + amount + " for: " + courseName);
        } else if ("REFUNDED".equals(status)) {
            request.setType("PAYMENT_REFUNDED");
            request.setTitle("Payment Refunded");
            request.setMessage("A refund of ₹" + amount + " has been processed for: " + courseName);
        }
        
        notificationService.sendNotification(request);
    }

    private UserProfileDto fetchStudent(Integer studentId) {
        try {
            return restTemplate.getForObject(authBaseUrl + "/api/v1/auth/profile/" + studentId, UserProfileDto.class);
        } catch (Exception e) {
            log.warn("Failed to fetch student profile for ID {}: {}", studentId, e.getMessage());
            return null;
        }
    }

    private CourseDto fetchCourse(Integer courseId) {
        try {
            return restTemplate.getForObject(courseBaseUrl + "/api/v1/courses/" + courseId, CourseDto.class);
        } catch (Exception e) {
            log.warn("Failed to fetch course details for ID {}: {}", courseId, e.getMessage());
            return null;
        }
    }
}

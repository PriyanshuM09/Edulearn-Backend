package com.edulearn.enrollment.messaging;

import com.edulearn.enrollment.event.EnrollmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendEnrollmentCreatedEvent(EnrollmentEvent event) {
        log.info("Sending enrollment created event to RabbitMQ: {}", event);
        rabbitTemplate.convertAndSend("edulearn.exchange", "enrollment.created", event);
    }

    public void sendEnrollmentCancelledEvent(EnrollmentEvent event) {
        log.info("Sending enrollment cancelled event to RabbitMQ: {}", event);
        rabbitTemplate.convertAndSend("edulearn.exchange", "enrollment.cancelled", event);
    }
}

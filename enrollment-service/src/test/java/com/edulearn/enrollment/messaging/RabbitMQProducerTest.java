package com.edulearn.enrollment.messaging;

import com.edulearn.enrollment.event.EnrollmentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMQProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void sendEnrollmentCreatedEvent_SendsExpectedRoutingKey() {
        RabbitMQProducer producer = new RabbitMQProducer(rabbitTemplate);
        EnrollmentEvent event = EnrollmentEvent.builder().enrollmentId(1).build();

        producer.sendEnrollmentCreatedEvent(event);

        verify(rabbitTemplate).convertAndSend("edulearn.exchange", "enrollment.created", event);
    }

    @Test
    void sendEnrollmentCancelledEvent_SendsExpectedRoutingKey() {
        RabbitMQProducer producer = new RabbitMQProducer(rabbitTemplate);
        EnrollmentEvent event = EnrollmentEvent.builder().enrollmentId(1).build();

        producer.sendEnrollmentCancelledEvent(event);

        verify(rabbitTemplate).convertAndSend("edulearn.exchange", "enrollment.cancelled", event);
    }
}

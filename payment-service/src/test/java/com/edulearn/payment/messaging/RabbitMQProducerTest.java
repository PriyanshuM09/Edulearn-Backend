package com.edulearn.payment.messaging;

import com.edulearn.payment.event.PaymentEvent;
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
    void sendPaymentSuccessEvent_SendsExpectedRoutingKey() {
        RabbitMQProducer producer = new RabbitMQProducer(rabbitTemplate);
        PaymentEvent event = PaymentEvent.builder().paymentId(1).build();

        producer.sendPaymentSuccessEvent(event);

        verify(rabbitTemplate).convertAndSend("edulearn.exchange", "payment.success", event);
    }

    @Test
    void sendPaymentRefundedEvent_SendsExpectedRoutingKey() {
        RabbitMQProducer producer = new RabbitMQProducer(rabbitTemplate);
        PaymentEvent event = PaymentEvent.builder().paymentId(1).build();

        producer.sendPaymentRefundedEvent(event);

        verify(rabbitTemplate).convertAndSend("edulearn.exchange", "payment.refunded", event);
    }
}

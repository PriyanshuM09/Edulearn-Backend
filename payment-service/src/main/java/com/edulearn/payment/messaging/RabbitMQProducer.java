package com.edulearn.payment.messaging;

import com.edulearn.payment.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendPaymentSuccessEvent(PaymentEvent event) {
        log.info("Sending payment success event to RabbitMQ: {}", event);
        rabbitTemplate.convertAndSend("edulearn.exchange", "payment.success", event);
    }

    public void sendPaymentRefundedEvent(PaymentEvent event) {
        log.info("Sending payment refunded event to RabbitMQ: {}", event);
        rabbitTemplate.convertAndSend("edulearn.exchange", "payment.refunded", event);
    }
}

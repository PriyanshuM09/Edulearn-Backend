package com.edulearn.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationConfigTest {

    @Test
    void rabbitMqConfig_CreatesExpectedBeans() {
        RabbitMQConfig config = new RabbitMQConfig();

        assertEquals("edulearn.exchange", config.exchange().getName());
        assertEquals("edulearn.enrollment.queue", config.enrollmentQueue().getName());
        assertEquals("edulearn.payment.queue", config.paymentQueue().getName());
        assertEquals("edulearn.enrollment.queue", config.enrollmentCreatedBinding().getDestination());
        assertEquals("edulearn.enrollment.queue", config.enrollmentCancelledBinding().getDestination());
        assertEquals("edulearn.payment.queue", config.paymentSuccessBinding().getDestination());
        assertEquals("edulearn.payment.queue", config.paymentRefundedBinding().getDestination());
        assertInstanceOf(Jackson2JsonMessageConverter.class, config.jsonMessageConverter());
    }

    @Test
    void swaggerConfig_CreatesOpenApiInfo() {
        assertTrue(new SwaggerConfig().notificationServiceOpenAPI().getInfo().getTitle()
                .contains("Notification Service API"));
    }
}

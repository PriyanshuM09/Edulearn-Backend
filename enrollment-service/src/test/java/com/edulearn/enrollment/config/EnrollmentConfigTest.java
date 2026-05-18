package com.edulearn.enrollment.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class EnrollmentConfigTest {

    @Test
    void rabbitMqConfig_CreatesExpectedBeans() {
        RabbitMQConfig config = new RabbitMQConfig();

        TopicExchange exchange = config.exchange();
        Queue queue = config.paymentQueue();
        Binding binding = config.paymentBinding();

        assertEquals("edulearn.exchange", exchange.getName());
        assertEquals("edulearn.payment.enrollment.queue", queue.getName());
        assertEquals("edulearn.payment.enrollment.queue", binding.getDestination());
        assertInstanceOf(Jackson2JsonMessageConverter.class, config.jsonMessageConverter());
    }

    @Test
    void swaggerConfig_CreatesOpenApiInfo() {
        SwaggerConfig config = new SwaggerConfig();

        assertEquals("Enrollment Service API",
                config.enrollmentServiceOpenAPI().getInfo().getTitle());
    }
}

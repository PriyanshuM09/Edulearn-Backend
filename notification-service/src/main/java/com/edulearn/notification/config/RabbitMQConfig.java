package com.edulearn.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("edulearn.exchange");
    }

    @Bean
    public Queue enrollmentQueue() {
        return new Queue("edulearn.enrollment.queue");
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue("edulearn.payment.queue");
    }

    @Bean
    public Binding enrollmentCreatedBinding() {
        return BindingBuilder.bind(enrollmentQueue()).to(exchange()).with("enrollment.created");
    }

    @Bean
    public Binding enrollmentCancelledBinding() {
        return BindingBuilder.bind(enrollmentQueue()).to(exchange()).with("enrollment.cancelled");
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentQueue()).to(exchange()).with("payment.success");
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentQueue()).to(exchange()).with("payment.refunded");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

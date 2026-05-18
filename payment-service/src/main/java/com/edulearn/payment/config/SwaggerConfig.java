package com.edulearn.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Payment Service API")
                        .description("Course purchases, subscriptions, refunds via Razorpay")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
package com.edulearn.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Notification Service API")
                        .description("In-app and email notifications "
                                + "with bulk dispatch support")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
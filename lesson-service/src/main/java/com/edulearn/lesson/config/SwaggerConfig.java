package com.edulearn.lesson.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI lessonServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Lesson/Content Service API")
                        .description("Manages lesson content, resource attachments, ordering, and preview access")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
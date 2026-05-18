package com.edulearn.progress.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI progressServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Progress Service API")
                        .description("Lesson-level progress tracking, " +
                                "course completion and PDF certificate generation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
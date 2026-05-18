package com.edulearn.course.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI courseServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Course Service API")
                        .description("Manages complete lifecycle of courses: creation, search, filtering, publishing")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
package com.edulearn.discussion.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI discussionServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduLearn — Discussion Service API")
                        .description("Course forums, threads, " +
                                "replies and moderation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("EduLearn Team")
                                .email("dev@edulearn.com")));
    }
}
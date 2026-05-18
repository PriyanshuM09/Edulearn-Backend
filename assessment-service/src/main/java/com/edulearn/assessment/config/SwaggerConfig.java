package com.edulearn.assessment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI assessmentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Assessment Service API")
                        .description(
                            "EduLearn Quiz and Attempt Management")
                        .version("v1.0.0"));
    }
}
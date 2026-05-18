package com.edulearn.progress.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /** Load-balanced RestTemplate — resolves service names via Eureka */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /** Direct RestTemplate — calls services by explicit localhost URL, no Eureka */
    @Bean
    @Qualifier("directRestTemplate")
    public RestTemplate directRestTemplate() {
        return new RestTemplate();
    }
}

package com.edulearn.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
            	    .requestMatchers(
            	        "/api/v1/auth/register",
            	        "/api/v1/auth/login",
            	        "/api/v1/auth/google-login",
            	        "/api/v1/auth/verify-email",
            	        "/api/v1/auth/validate",
            	        "/api/v1/auth/refresh",
            	        "/api/v1/auth/forgot-password",
            	        "/api/v1/auth/reset-password",
            	        "/api/v1/auth/test-email",
            	        "/api/v1/auth/profile/**",
            	        "/actuator/**",
            	        "/swagger-ui/**",
            	        "/swagger-ui.html",
            	        "/v3/api-docs/**"
            	    ).permitAll()
            	    .requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
            	    .anyRequest().authenticated()
            	)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
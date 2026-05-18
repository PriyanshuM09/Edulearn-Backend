package com.edulearn.gateway;

import com.edulearn.gateway.filter.JwtAuthFilter;
import com.edulearn.gateway.filter.LoggingFilter;
import com.edulearn.gateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiGatewayCoverageTest {

    private JwtUtil jwtUtil;
    private String secret = "v8y/B?E(H+MbQeThWmZq4t7w!z%C&F)J@NcRfUjXn2r5u8x/A?D(G-KaPdSgVkYp";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", secret);
    }

    @Test
    void jwtUtilValidatesAndExtractsClaims() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("testuser")
                .claim("role", "STUDENT")
                .claim("userId", "123")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("testuser", jwtUtil.extractUsername(token));
        assertEquals("STUDENT", jwtUtil.extractRole(token));
        assertEquals("123", jwtUtil.extractUserId(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void jwtUtilHandlesInvalidTokens() {
        assertFalse(jwtUtil.validateToken("invalid-token"));
        assertFalse(jwtUtil.validateToken(""));
        
        // Expired token
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();
        assertFalse(jwtUtil.validateToken(expiredToken));
    }

    @Test
    void jwtAuthFilterAllowsPublicEndpoints() {
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertEquals(-1, filter.getOrder());
    }

    @Test
    void jwtAuthFilterRejectsMissingToken() {
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/courses").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void jwtAuthFilterAllowsValidTokenWithRole() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("testuser")
                .claim("role", "STUDENT")
                .claim("userId", "123")
                .signWith(key)
                .compact();

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(chain.filter(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        ServerWebExchange mutatedExchange = captor.getValue();
        assertEquals("testuser", mutatedExchange.getRequest().getHeaders().getFirst("X-User-Username"));
        assertEquals("STUDENT", mutatedExchange.getRequest().getHeaders().getFirst("X-User-Role"));
        assertEquals("123", mutatedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void jwtAuthFilterRejectsForbiddenRole() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("testuser")
                .claim("role", "STUDENT")
                .signWith(key)
                .compact();

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        // Student trying to POST to courses
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void jwtAuthFilterAllowsAdminEverything() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("adminuser")
                .claim("role", "ADMIN")
                .signWith(key)
                .compact();

        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/courses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
    }

    @Test
    void jwtAuthFilterHandlesOptionsRequest() {
        JwtAuthFilter filter = new JwtAuthFilter(jwtUtil);
        MockServerHttpRequest request = MockServerHttpRequest.options("/api/v1/courses").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void loggingFilterLogsRequestAndResponse() {
        LoggingFilter filter = new LoggingFilter();
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(chain).filter(any());
        assertEquals(-2, filter.getOrder());
    }

    @Test
    void apiGatewayApplicationMainRuns() {
        try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
            String[] args = {"--test"};
            ApiGatewayApplication.main(args);
            spring.verify(() -> SpringApplication.run(ApiGatewayApplication.class, args));
        }
    }

}

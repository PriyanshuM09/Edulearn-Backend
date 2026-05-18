package com.edulearn.auth.config;

import com.edulearn.auth.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtFilter, "jwtSecret", "mySecretKeyMySecretKeyMySecretKeyMySecretKey");
    }

    @Test
    @DisplayName("Filter - No Auth Header")
    void doFilter_NoHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter - Invalid Header Format")
    void doFilter_InvalidHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic 123");
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter - Blacklisted Token")
    void doFilter_Blacklisted() throws Exception {
        String token = "mock-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authService.validateToken(token)).thenReturn(false);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter - Exception Handling (Invalid JWT)")
    void doFilter_Exception() throws Exception {
        String token = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authService.validateToken(token)).thenReturn(true);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        // Should catch exception and continue
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Filter - Success Path")
    void doFilter_Success() throws Exception {
        // We need a token that can be parsed or we need to mock the parsing
        // Since it uses Jwts.parser(), it's hard to mock static.
        // But we can provide a token signed with the same secret.
        String secret = "mySecretKeyMySecretKeyMySecretKeyMySecretKey";
        String token = io.jsonwebtoken.Jwts.builder()
                .setSubject("test@test.com")
                .claim("role", "STUDENT")
                .claim("userId", 1)
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(authService.validateToken(token)).thenReturn(true);
        
        jwtFilter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(request, response);
        assert org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null;
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}

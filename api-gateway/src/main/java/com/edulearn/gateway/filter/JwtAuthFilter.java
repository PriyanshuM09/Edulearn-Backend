package com.edulearn.gateway.filter;

import com.edulearn.gateway.exception.ForbiddenException;
import com.edulearn.gateway.exception.JwtValidationException;
import com.edulearn.gateway.exception.UnauthorizedException;
import com.edulearn.gateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // ── Roles ────────────────────────────────────────────────────────────
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_STUDENT = "STUDENT";

    // ── Public endpoints — NO token required ─────────────────────────────
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/google-login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh",
            "/api/v1/auth/test-email",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/profile",
            "/api/v1/courses",
            "/api/v1/progress/certificates/verify", 
            "/actuator/health",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/webjars"
    );

    // ── Role-based access control map ────────────────────────────────────
    // Path prefix → allowed roles
    private static final Map<String, List<String>> ROLE_ACCESS = Map.ofEntries(
            Map.entry("/api/v1/auth",          List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/payments",      List.of(ROLE_STUDENT, ROLE_ADMIN)),
            Map.entry("/api/v1/enrollments",   List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/courses",       List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/lessons",       List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/quizzes",       List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/questions",     List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/attempts",      List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/progress",      List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/discussions",   List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN)),
            Map.entry("/api/v1/notifications", List.of(ROLE_STUDENT, ROLE_INSTRUCTOR, ROLE_ADMIN))
    );

    // ── Instructor-only write operations ─────────────────────────────────
    private static final List<String> INSTRUCTOR_ONLY_PATHS = List.of(
            "/api/v1/courses",
            "/api/v1/lessons",
            "/api/v1/quizzes",
            "/api/v1/questions"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();
        String method = request.getMethod().name();

        log.debug("Incoming request: {} {}", method, path);

        // ── Step 0: Handle OPTIONS (CORS preflight) ───────────────────────
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            return exchange.getResponse().setComplete();
        }

        // ── Step 1: Is it a public endpoint? ─────────────────────────────
        if (isPublicEndpoint(path)) {
            // Special case: Only GET is public for courses
            if (path.startsWith("/api/v1/courses") && !"GET".equalsIgnoreCase(method)) {
                log.debug("Non-GET request on public course path — requiring auth: {}", path);
            } else {
                log.debug("Public endpoint — skipping auth: {}", path);
                return chain.filter(exchange);
            }
        }

        // ── Step 2: Extract Authorization header ──────────────────────────────
        String authHeader = request.getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for: {}", path);
            throw new UnauthorizedException("Authorization header missing or invalid");
        }

        // ── Step 3: Extract and validate JWT token ────────────────────────────
        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            throw new JwtValidationException("Invalid or expired JWT token");
        }

        // ── Step 4: Extract claims from token ─────────────────────────────────
        String username = jwtUtil.extractUsername(token);
        String role     = jwtUtil.extractRole(token);
        String userId   = jwtUtil.extractUserId(token);

        log.debug("Authenticated user: {} (Role: {}, ID: {}) for path: {}",
                username, role, userId, path);

        // ── Step 5: Role-based access check ───────────────────────────────────
        if (!isAuthorized(path, method, role)) {
            log.warn("Access denied — user: {} role: {} path: {} method: {}",
                    username, role, path, method);
            throw new ForbiddenException("Access denied. Insufficient role: " + role);
        }

        // ── Step 6: Forward user info to downstream services ──────────────────
        ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-User-Username", username != null ? username : "")
                .header("X-User-Role",     role != null ? role : "")
                .header("X-User-Id",       userId != null ? userId : "")
                .build();

        log.debug("Request forwarded to downstream — user: {} role: {}",
                username, role);

        return chain.filter(exchange.mutate()
                .request(mutatedRequest)
                .build());
    }

    // ── Check if path is public ───────────────────────────────────────────
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(path::startsWith);
    }

    // ── Role + method based authorization ────────────────────────────────
    private boolean isAuthorized(String path, String method, String role) {

        // ── Step 0: ADMIN can access everything ───────────────────────────
        if (ROLE_ADMIN.equals(role)) return true;

        // ── Step 1: Exception for reviews (Students/Instructors can POST) ──
        if ("POST".equalsIgnoreCase(method) && path.contains("/reviews")) {
            log.debug("Allowing POST on review path: {}", path);
            return true;
        }

        // ── Step 2: Check general role access map ──────────────────────────
        boolean roleAllowed = ROLE_ACCESS.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .anyMatch(entry -> entry.getValue().contains(role));

        if (!roleAllowed) {
            log.warn("Role {} not allowed for path: {}", role, path);
            return false;
        }

        // ── Step 3: Instructor-only write operations ───────────────────────
        // STUDENT cannot create/update/delete courses or lessons
        if (ROLE_STUDENT.equals(role)) {
            boolean isWriteOperation = List.of("POST", "PUT", "DELETE")
                    .contains(method);
            boolean isRestrictedPath = INSTRUCTOR_ONLY_PATHS.stream()
                    .anyMatch(path::startsWith);
            
            if (isWriteOperation && isRestrictedPath) {
                log.warn("{} attempted write on restricted path: {}",
                        ROLE_STUDENT, path);
                return false;
            }
        }

        return true;
    }


    // ── Run this filter first — before all others ────────────────────────────────────
    @Override
    public int getOrder() {
        return -1;
    }
}
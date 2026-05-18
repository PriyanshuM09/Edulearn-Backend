package com.edulearn.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Global exception handler for the API Gateway (Spring WebFlux).
 *
 * This is the reactive equivalent of @RestControllerAdvice used in other services.
 * It runs before Spring's DefaultErrorWebExceptionHandler (Order -2 vs -1).
 *
 * Handles:
 *  - GatewayException (and all subclasses: JwtValidationException, UnauthorizedException, ForbiddenException)
 *  - ResponseStatusException (404 route not found, etc.)
 *  - Generic Exception (fallback 500)
 */
@Slf4j
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalErrorHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().toString();

        // ── GatewayException and all subclasses ───────────────────────────
        if (ex instanceof GatewayException gatewayEx) {
            log.warn("Gateway exception on [{}]: {} - {}",
                    path, gatewayEx.getStatus(), gatewayEx.getMessage());
            return writeErrorResponse(exchange, gatewayEx.getStatus(), gatewayEx.getMessage(), path);
        }

        // ── Spring's own routing exceptions (e.g. 404 No route found) ─────
        if (ex instanceof ResponseStatusException responseEx) {
            HttpStatus status = HttpStatus.resolve(responseEx.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            log.warn("Response status exception on [{}]: {}", path, responseEx.getMessage());
            return writeErrorResponse(exchange, status, responseEx.getReason() != null
                    ? responseEx.getReason() : status.getReasonPhrase(), path);
        }

        // ── IllegalArgumentException ───────────────────────────────────────
        if (ex instanceof IllegalArgumentException illegalEx) {
            log.warn("Illegal argument on [{}]: {}", path, illegalEx.getMessage());
            return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, illegalEx.getMessage(), path);
        }

        // ── IllegalStateException ─────────────────────────────────────────
        if (ex instanceof IllegalStateException illegalStateEx) {
            log.warn("Illegal state on [{}]: {}", path, illegalStateEx.getMessage());
            return writeErrorResponse(exchange, HttpStatus.BAD_REQUEST, illegalStateEx.getMessage(), path);
        }

        // ── Fallback: Generic 500 ─────────────────────────────────────────
        log.error("Unhandled exception on [{}]: {}", path, ex.getMessage(), ex);
        return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected gateway error occurred", path);
    }

    // ── Build and write JSON error response ───────────────────────────────
    private Mono<Void> writeErrorResponse(ServerWebExchange exchange,
                                           HttpStatus status,
                                           String message,
                                           String path) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now(),
                path
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response: {}", e.getMessage());
            byte[] fallback = ("{\"status\":500,\"error\":\"Internal Server Error\"," +
                    "\"message\":\"Error serialization failed\"}")
                    .getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(fallback);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        }
    }
}

package com.edulearn.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ── Get signing key from secret string ──────────────────────────────
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Validate token — returns true if valid ───────────────────────────
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT token malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT token empty: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    // ── Extract all claims ───────────────────────────────────────────────
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Extract username/email ───────────────────────────────────────────
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ── Extract role ─────────────────────────────────────────────────────
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    // ── Extract userId ───────────────────────────────────────────────────
    public String extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    // ── Check expiry ─────────────────────────────────────────────────────
    public boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }
}
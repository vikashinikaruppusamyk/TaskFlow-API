package com.example.todoApp.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final String SECRET = "your-256-bit-secret-key-which-should-be-very-long";
    private final long EXPIRATION_TIME = 1000 * 60 * 60;

    private final Key SECRET_KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // generate token
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    // extract email/username
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    // validate token
    public boolean validateJwtToken(String token) {
        return validateJwtTokenWithReason(token) == null;
    }

    // returns null when token is valid, otherwise a reason string
    public String validateJwtTokenWithReason(String token) {
        try {
            getClaims(token);
            return null;
        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed: token expired at {}", e.getClaims().getExpiration());
            return "token expired";
        } catch (UnsupportedJwtException e) {
            log.warn("JWT validation failed: unsupported token");
            return "unsupported token";
        } catch (MalformedJwtException e) {
            log.warn("JWT validation failed: malformed token");
            return "malformed token";
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT validation failed: invalid signature");
            return "invalid signature";
        } catch (IllegalArgumentException e) {
            log.warn("JWT validation failed: empty/blank token");
            return "empty/blank token";
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return "jwt exception: " + e.getClass().getSimpleName();
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

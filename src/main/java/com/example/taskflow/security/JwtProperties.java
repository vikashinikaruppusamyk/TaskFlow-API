package com.example.taskflow.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * JWT settings, bound from {@code taskflow.jwt.*}. Validated at startup, so a missing or
 * too-short secret stops the application instead of producing weak tokens.
 */
@Validated
@ConfigurationProperties("taskflow.jwt")
public record JwtProperties(
        @NotBlank(message = "must be set (JWT_SECRET)")
        @Size(min = 32, message = "must be at least 32 characters (JWT_SECRET)")
        String secret,
        @NotNull Duration expiration,
        @NotBlank String issuer) {
}

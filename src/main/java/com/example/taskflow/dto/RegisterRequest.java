package com.example.taskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "alex@example.com")
        @NotBlank @Email @Size(max = 254)
        String email,

        // BCrypt only uses the first 72 bytes of a password, so longer ones are rejected rather than truncated
        @Schema(example = "correct-horse-battery")
        @NotBlank @Size(min = 8, max = 72)
        String password) {

    // Trimmed before validation runs, so " alex@example.com " is accepted as a valid email
    public RegisterRequest {
        email = email == null ? null : email.strip();
    }
}

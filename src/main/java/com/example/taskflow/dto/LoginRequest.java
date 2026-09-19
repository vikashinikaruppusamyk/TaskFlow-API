package com.example.taskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "alex@example.com") @NotBlank String email,
        @Schema(example = "correct-horse-battery") @NotBlank String password) {

    public LoginRequest {
        email = email == null ? null : email.strip();
    }
}

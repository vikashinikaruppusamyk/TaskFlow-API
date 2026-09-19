package com.example.taskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @Schema(example = "Write project README") @NotBlank @Size(max = 200) String title,
        @Schema(example = "Cover setup, API overview and the event log") @Size(max = 2000) String description) {
}

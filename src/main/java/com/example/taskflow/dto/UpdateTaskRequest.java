package com.example.taskflow.dto;

import com.example.taskflow.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Full replacement of a task's editable fields (PUT semantics). */
public record UpdateTaskRequest(
        @Schema(example = "Write project README") @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull TaskStatus status) {
}

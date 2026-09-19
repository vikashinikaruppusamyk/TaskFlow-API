package com.example.taskflow.dto;

import com.example.taskflow.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull TaskStatus status) {
}

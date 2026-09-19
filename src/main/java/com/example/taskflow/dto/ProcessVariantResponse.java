package com.example.taskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** A process variant: one distinct sequence of activities, and how many cases followed it. */
public record ProcessVariantResponse(
        @Schema(example = "[\"Create Task\", \"Start Work\", \"Complete Task\"]") List<String> activities,
        long caseCount,
        @Schema(description = "Share of all cases, in percent", example = "62.5") double percentage) {
}

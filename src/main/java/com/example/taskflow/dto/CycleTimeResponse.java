package com.example.taskflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Time from "Create Task" to the last "Complete Task" of each completed case, in seconds.
 * The statistics are null when no case has been completed yet.
 */
public record CycleTimeResponse(
        @Schema(description = "Number of cases that reached Complete Task at least once") int completedCases,
        Long averageSeconds,
        Long medianSeconds,
        Long minSeconds,
        Long maxSeconds,
        @Schema(description = "Average as an ISO-8601 duration", example = "PT2H30M") String average) {
}

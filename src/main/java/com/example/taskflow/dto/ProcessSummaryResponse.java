package com.example.taskflow.dto;

import com.example.taskflow.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ProcessSummaryResponse(
        @Schema(description = "All cases in the event log, including deleted tasks") long totalCases,
        @Schema(description = "Current tasks that are not completed") long openCases,
        @Schema(description = "Cases that reached Complete Task at least once") long completedCases,
        long reopenedCases,
        long deletedCases,
        @Schema(description = "Reopened cases as a percentage of completed cases", example = "12.5") double reworkRate,
        @Schema(description = "Current (not deleted) tasks per status") Map<TaskStatus, Long> tasksByStatus) {
}

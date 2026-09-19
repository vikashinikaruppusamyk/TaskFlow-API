package com.example.taskflow.controller;

import com.example.taskflow.dto.CycleTimeResponse;
import com.example.taskflow.dto.ProcessSummaryResponse;
import com.example.taskflow.dto.ProcessVariantResponse;
import com.example.taskflow.dto.TaskEventResponse;
import com.example.taskflow.security.AuthenticatedUser;
import com.example.taskflow.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Process analytics",
        description = "Process mining over your event log: each task is a case, each change an activity")
@ApiResponse(responseCode = "401", description = "Missing, expired or invalid access token")
public class AnalyticsController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv");

    private final AnalyticsService analyticsService;

    @GetMapping("/event-log")
    @Operation(summary = "Your full event log, grouped by case and ordered by time")
    public List<TaskEventResponse> eventLog(@AuthenticationPrincipal AuthenticatedUser user) {
        return analyticsService.eventLog(user.id());
    }

    @GetMapping(value = "/event-log/export", produces = "text/csv")
    @Operation(summary = "Download the event log as CSV",
            description = "Columns: case_id, activity, timestamp, from_status, to_status. "
                    + "The format can be imported into a process-mining tool.")
    public ResponseEntity<String> exportEventLog(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("taskflow-event-log.csv").build().toString())
                .body(analyticsService.eventLogCsv(user.id()));
    }

    @GetMapping("/summary")
    @Operation(summary = "Case counts, current tasks per status, and rework rate")
    public ProcessSummaryResponse summary(@AuthenticationPrincipal AuthenticatedUser user) {
        return analyticsService.summary(user.id());
    }

    @GetMapping("/cycle-time")
    @Operation(summary = "Cycle time from Create Task to Complete Task",
            description = "Average, median, min and max over all cases that were completed.")
    public CycleTimeResponse cycleTime(@AuthenticationPrincipal AuthenticatedUser user) {
        return analyticsService.cycleTime(user.id());
    }

    @GetMapping("/variants")
    @Operation(summary = "Process variants: distinct activity sequences, most common first")
    public List<ProcessVariantResponse> variants(@AuthenticationPrincipal AuthenticatedUser user) {
        return analyticsService.variants(user.id());
    }
}

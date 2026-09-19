package com.example.taskflow.dto;

import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;
import com.example.taskflow.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** One event-log entry in the classic process-mining shape: case id, activity, timestamp. */
public record TaskEventResponse(
        @Schema(description = "The case: the id of the task this event belongs to") Long caseId,
        TaskActivity activity,
        @Schema(example = "Start Work") String activityLabel,
        TaskStatus fromStatus,
        TaskStatus toStatus,
        Instant timestamp) {

    public static TaskEventResponse from(TaskEvent event) {
        return new TaskEventResponse(event.getTaskId(), event.getActivity(), event.getActivity().getLabel(),
                event.getFromStatus(), event.getToStatus(), event.getOccurredAt());
    }
}

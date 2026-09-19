package com.example.taskflow.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An activity in a task's lifecycle. Each task is a "case" in the process-mining sense, and every
 * activity performed on it is written to the event log together with a timestamp.
 */
@Getter
@RequiredArgsConstructor
public enum TaskActivity {
    CREATED("Create Task"),
    STARTED("Start Work"),
    PAUSED("Pause Work"),
    COMPLETED("Complete Task"),
    REOPENED("Reopen Task"),
    DETAILS_UPDATED("Update Details"),
    DELETED("Delete Task");

    /** Human-readable activity name, as it appears in the exported event log. */
    private final String label;

    /** Names the activity that moves a task from one status to another. */
    public static TaskActivity forStatusChange(TaskStatus from, TaskStatus to) {
        if (from == to) {
            throw new IllegalArgumentException("Status did not change: " + from);
        }
        if (to == TaskStatus.COMPLETED) {
            return COMPLETED;
        }
        if (from == TaskStatus.COMPLETED) {
            return REOPENED;
        }
        return to == TaskStatus.IN_PROGRESS ? STARTED : PAUSED;
    }
}

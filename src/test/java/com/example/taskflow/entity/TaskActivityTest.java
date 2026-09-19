package com.example.taskflow.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TaskActivityTest {

    @ParameterizedTest(name = "{0} -> {1} is {2}")
    @CsvSource({
            "TODO,        IN_PROGRESS, STARTED",
            "IN_PROGRESS, TODO,        PAUSED",
            "TODO,        COMPLETED,   COMPLETED",
            "IN_PROGRESS, COMPLETED,   COMPLETED",
            "COMPLETED,   TODO,        REOPENED",
            "COMPLETED,   IN_PROGRESS, REOPENED"
    })
    void statusChangesMapToActivities(TaskStatus from, TaskStatus to, TaskActivity expected) {
        assertThat(TaskActivity.forStatusChange(from, to)).isEqualTo(expected);
    }

    @Test
    void unchangedStatusIsNotAnActivity() {
        assertThatIllegalArgumentException().isThrownBy(() -> TaskActivity.forStatusChange(TaskStatus.TODO, TaskStatus.TODO));
    }
}

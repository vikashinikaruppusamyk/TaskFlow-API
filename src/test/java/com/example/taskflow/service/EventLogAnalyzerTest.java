package com.example.taskflow.service;

import com.example.taskflow.dto.CycleTimeResponse;
import com.example.taskflow.dto.ProcessVariantResponse;
import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.example.taskflow.entity.TaskActivity.COMPLETED;
import static com.example.taskflow.entity.TaskActivity.CREATED;
import static com.example.taskflow.entity.TaskActivity.DELETED;
import static com.example.taskflow.entity.TaskActivity.REOPENED;
import static com.example.taskflow.entity.TaskActivity.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

class EventLogAnalyzerTest {

    private static final Instant T0 = Instant.parse("2026-01-05T09:00:00Z");

    private final List<TaskEvent> log = new ArrayList<>();

    private void event(long caseId, TaskActivity activity, Duration afterStart) {
        log.add(new TaskEvent(caseId, 1L, activity, null, null, T0.plus(afterStart)));
    }

    @Test
    void emptyLogHasNoCasesAndNoCycleTime() {
        EventLogAnalyzer analyzer = new EventLogAnalyzer(List.of());

        assertThat(analyzer.caseCount()).isZero();
        assertThat(analyzer.variants()).isEmpty();
        assertThat(analyzer.cycleTime()).isEqualTo(new CycleTimeResponse(0, null, null, null, null, null));
    }

    @Test
    void cycleTimeUsesFirstCreationAndLastCompletion() {
        event(1, CREATED, Duration.ZERO);
        event(1, COMPLETED, Duration.ofHours(1));
        event(1, REOPENED, Duration.ofHours(2));
        event(1, COMPLETED, Duration.ofHours(5));

        assertThat(new EventLogAnalyzer(log).cycleTime().averageSeconds()).isEqualTo(Duration.ofHours(5).toSeconds());
    }

    @Test
    void cycleTimeStatisticsSkipUnfinishedCases() {
        event(1, CREATED, Duration.ZERO);
        event(1, COMPLETED, Duration.ofHours(1));
        event(2, CREATED, Duration.ZERO);
        event(2, COMPLETED, Duration.ofHours(2));
        event(3, CREATED, Duration.ZERO);
        event(3, COMPLETED, Duration.ofHours(6));
        event(4, CREATED, Duration.ZERO);
        event(4, STARTED, Duration.ofHours(1)); // never completed

        CycleTimeResponse cycleTime = new EventLogAnalyzer(log).cycleTime();

        assertThat(cycleTime.completedCases()).isEqualTo(3);
        assertThat(cycleTime.averageSeconds()).isEqualTo(3 * 3600);
        assertThat(cycleTime.medianSeconds()).isEqualTo(2 * 3600);
        assertThat(cycleTime.minSeconds()).isEqualTo(3600);
        assertThat(cycleTime.maxSeconds()).isEqualTo(6 * 3600);
        assertThat(cycleTime.average()).isEqualTo("PT3H");
    }

    @Test
    void medianOfEvenCountIsMeanOfMiddleValues() {
        event(1, CREATED, Duration.ZERO);
        event(1, COMPLETED, Duration.ofHours(1));
        event(2, CREATED, Duration.ZERO);
        event(2, COMPLETED, Duration.ofHours(4));

        assertThat(new EventLogAnalyzer(log).cycleTime().medianSeconds()).isEqualTo(Duration.ofMinutes(150).toSeconds());
    }

    @Test
    void eventsAreOrderedByTimeWithinACaseEvenIfLoggedOutOfOrder() {
        event(1, COMPLETED, Duration.ofHours(2));
        event(1, CREATED, Duration.ZERO);
        event(1, STARTED, Duration.ofHours(1));

        assertThat(new EventLogAnalyzer(log).variants())
                .extracting(ProcessVariantResponse::activities)
                .containsExactly(List.of("Create Task", "Start Work", "Complete Task"));
    }

    @Test
    void variantsAreSortedByFrequencyWithPercentages() {
        event(1, CREATED, Duration.ZERO);
        event(1, DELETED, Duration.ofMinutes(1));
        for (long caseId = 2; caseId <= 3; caseId++) {
            event(caseId, CREATED, Duration.ZERO);
            event(caseId, STARTED, Duration.ofMinutes(1));
            event(caseId, COMPLETED, Duration.ofMinutes(2));
        }

        List<ProcessVariantResponse> variants = new EventLogAnalyzer(log).variants();

        assertThat(variants).containsExactly(
                new ProcessVariantResponse(List.of("Create Task", "Start Work", "Complete Task"), 2, 66.7),
                new ProcessVariantResponse(List.of("Create Task", "Delete Task"), 1, 33.3));
    }

    @Test
    void casesWithCountsEachCaseOnce() {
        event(1, CREATED, Duration.ZERO);
        event(1, COMPLETED, Duration.ofMinutes(1));
        event(1, REOPENED, Duration.ofMinutes(2));
        event(1, COMPLETED, Duration.ofMinutes(3));
        event(2, CREATED, Duration.ZERO);

        EventLogAnalyzer analyzer = new EventLogAnalyzer(log);

        assertThat(analyzer.caseCount()).isEqualTo(2);
        assertThat(analyzer.casesWith(COMPLETED)).isEqualTo(1);
        assertThat(analyzer.casesWith(REOPENED)).isEqualTo(1);
    }

    @Test
    void percentageRoundsToOneDecimalAndHandlesZeroTotal() {
        assertThat(EventLogAnalyzer.percentage(1, 3)).isEqualTo(33.3);
        assertThat(EventLogAnalyzer.percentage(2, 3)).isEqualTo(66.7);
        assertThat(EventLogAnalyzer.percentage(5, 0)).isZero();
    }
}

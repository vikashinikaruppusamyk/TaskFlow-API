package com.example.taskflow.service;

import com.example.taskflow.dto.CycleTimeResponse;
import com.example.taskflow.dto.ProcessVariantResponse;
import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Process-mining calculations over an event log. Events are grouped into cases by task id and ordered by time,
 * which gives each case a trace (its sequence of activities). Pure Java with no Spring or database access,
 * so the calculations are unit-tested directly.
 */
public final class EventLogAnalyzer {

    private final Map<Long, List<TaskEvent>> tracesByCase = new LinkedHashMap<>();

    public EventLogAnalyzer(List<TaskEvent> events) {
        for (TaskEvent event : events) {
            tracesByCase.computeIfAbsent(event.getTaskId(), caseId -> new ArrayList<>()).add(event);
        }
        // Stable sort: events with the same timestamp keep the order they were recorded in
        tracesByCase.values().forEach(trace -> trace.sort(Comparator.comparing(TaskEvent::getOccurredAt)));
    }

    public long caseCount() {
        return tracesByCase.size();
    }

    public long casesWith(TaskActivity activity) {
        return tracesByCase.values().stream()
                .filter(trace -> trace.stream().anyMatch(event -> event.getActivity() == activity))
                .count();
    }

    /** Cycle time of a case = first "Create Task" to last "Complete Task". Cases never completed are skipped. */
    public CycleTimeResponse cycleTime() {
        List<Long> seconds = tracesByCase.values().stream()
                .map(EventLogAnalyzer::cycleTimeOf)
                .flatMap(Optional::stream)
                .map(Duration::toSeconds)
                .sorted()
                .toList();
        if (seconds.isEmpty()) {
            return new CycleTimeResponse(0, null, null, null, null, null);
        }
        long average = Math.round(seconds.stream().mapToLong(Long::longValue).average().orElseThrow());
        return new CycleTimeResponse(seconds.size(), average, median(seconds),
                seconds.get(0), seconds.get(seconds.size() - 1), Duration.ofSeconds(average).toString());
    }

    /** Distinct activity sequences, most common first; ties keep the order in which the variant first appeared. */
    public List<ProcessVariantResponse> variants() {
        Map<List<String>, Long> counts = new LinkedHashMap<>();
        for (List<TaskEvent> trace : tracesByCase.values()) {
            List<String> activities = trace.stream().map(event -> event.getActivity().getLabel()).toList();
            counts.merge(activities, 1L, Long::sum);
        }
        long total = caseCount();
        return counts.entrySet().stream()
                .sorted(Map.Entry.<List<String>, Long>comparingByValue().reversed())
                .map(entry -> new ProcessVariantResponse(entry.getKey(), entry.getValue(),
                        percentage(entry.getValue(), total)))
                .toList();
    }

    static Optional<Duration> cycleTimeOf(List<TaskEvent> trace) {
        Optional<Instant> created = trace.stream()
                .filter(event -> event.getActivity() == TaskActivity.CREATED)
                .map(TaskEvent::getOccurredAt)
                .findFirst();
        Optional<Instant> lastCompleted = trace.stream()
                .filter(event -> event.getActivity() == TaskActivity.COMPLETED)
                .map(TaskEvent::getOccurredAt)
                .reduce((first, second) -> second);
        if (created.isEmpty() || lastCompleted.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(created.get(), lastCompleted.get()));
    }

    /** Percentage rounded to one decimal place; 0 when there is nothing to divide by. */
    static double percentage(long part, long total) {
        return total == 0 ? 0.0 : Math.round(part * 1000.0 / total) / 10.0;
    }

    private static long median(List<Long> sorted) {
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 1
                ? sorted.get(middle)
                : Math.round((sorted.get(middle - 1) + sorted.get(middle)) / 2.0);
    }
}

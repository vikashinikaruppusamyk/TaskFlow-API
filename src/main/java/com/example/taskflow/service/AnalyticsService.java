package com.example.taskflow.service;

import com.example.taskflow.dto.CycleTimeResponse;
import com.example.taskflow.dto.ProcessSummaryResponse;
import com.example.taskflow.dto.ProcessVariantResponse;
import com.example.taskflow.dto.TaskEventResponse;
import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.repository.TaskEventRepository;
import com.example.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Mines the caller's own event log. Users only ever see analytics over their own tasks. */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final String CSV_HEADER = "case_id,activity,timestamp,from_status,to_status";

    private final TaskEventRepository eventRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<TaskEventResponse> eventLog(Long ownerId) {
        return events(ownerId).stream().map(TaskEventResponse::from).toList();
    }

    /** The event log as CSV, ready to import into a process-mining tool (case id, activity, timestamp). */
    @Transactional(readOnly = true)
    public String eventLogCsv(Long ownerId) {
        StringBuilder csv = new StringBuilder(CSV_HEADER).append('\n');
        for (TaskEvent event : events(ownerId)) {
            csv.append(event.getTaskId()).append(',')
                    .append(csvValue(event.getActivity().getLabel())).append(',')
                    .append(event.getOccurredAt()).append(',')
                    .append(event.getFromStatus() == null ? "" : event.getFromStatus()).append(',')
                    .append(event.getToStatus() == null ? "" : event.getToStatus()).append('\n');
        }
        return csv.toString();
    }

    @Transactional(readOnly = true)
    public CycleTimeResponse cycleTime(Long ownerId) {
        return analyzer(ownerId).cycleTime();
    }

    @Transactional(readOnly = true)
    public List<ProcessVariantResponse> variants(Long ownerId) {
        return analyzer(ownerId).variants();
    }

    @Transactional(readOnly = true)
    public ProcessSummaryResponse summary(Long ownerId) {
        EventLogAnalyzer analyzer = analyzer(ownerId);

        Map<TaskStatus, Long> tasksByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, 0L);
        }
        taskRepository.countByStatus(ownerId).forEach(row -> tasksByStatus.put(row.getStatus(), row.getCount()));

        long completed = analyzer.casesWith(TaskActivity.COMPLETED);
        long reopened = analyzer.casesWith(TaskActivity.REOPENED);
        return new ProcessSummaryResponse(
                analyzer.caseCount(),
                tasksByStatus.get(TaskStatus.TODO) + tasksByStatus.get(TaskStatus.IN_PROGRESS),
                completed,
                reopened,
                analyzer.casesWith(TaskActivity.DELETED),
                EventLogAnalyzer.percentage(reopened, completed),
                tasksByStatus);
    }

    private EventLogAnalyzer analyzer(Long ownerId) {
        return new EventLogAnalyzer(events(ownerId));
    }

    private List<TaskEvent> events(Long ownerId) {
        return eventRepository.findByOwnerIdOrderByTaskIdAscOccurredAtAscIdAsc(ownerId);
    }

    private static String csvValue(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}

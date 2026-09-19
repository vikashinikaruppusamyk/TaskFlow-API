package com.example.taskflow.service;

import com.example.taskflow.dto.CreateTaskRequest;
import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.TaskEventResponse;
import com.example.taskflow.dto.TaskResponse;
import com.example.taskflow.dto.UpdateTaskRequest;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.exception.TaskNotFoundException;
import com.example.taskflow.repository.TaskEventRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Task operations for one owner. Every change is written to the event log in the same transaction as the
 * change itself, so the log can never disagree with the task data.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TaskRepository taskRepository;
    private final TaskEventRepository eventRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public TaskResponse create(Long ownerId, CreateTaskRequest request) {
        Instant now = clock.instant();
        Task task = taskRepository.save(new Task(userRepository.getReferenceById(ownerId),
                request.title().strip(), normalizeDescription(request.description()), now));
        record(task, ownerId, TaskActivity.CREATED, null, TaskStatus.TODO, now);
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long ownerId, Long taskId) {
        return TaskResponse.from(findOwnedTask(ownerId, taskId));
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Long ownerId, TaskStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Task> tasks = status == null
                ? taskRepository.findByOwnerId(ownerId, pageRequest)
                : taskRepository.findByOwnerIdAndStatus(ownerId, status, pageRequest);
        return PageResponse.from(tasks, TaskResponse::from);
    }

    @Transactional
    public TaskResponse update(Long ownerId, Long taskId, UpdateTaskRequest request) {
        Task task = findOwnedTask(ownerId, taskId);
        Instant now = clock.instant();
        if (task.updateDetails(request.title().strip(), normalizeDescription(request.description()), now)) {
            record(task, ownerId, TaskActivity.DETAILS_UPDATED, task.getStatus(), task.getStatus(), now);
        }
        applyStatus(task, ownerId, request.status(), now);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse changeStatus(Long ownerId, Long taskId, TaskStatus status) {
        Task task = findOwnedTask(ownerId, taskId);
        applyStatus(task, ownerId, status, clock.instant());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long ownerId, Long taskId) {
        Task task = findOwnedTask(ownerId, taskId);
        record(task, ownerId, TaskActivity.DELETED, task.getStatus(), null, clock.instant());
        taskRepository.delete(task);
    }

    /** The full event history of one case. Still available after the task itself was deleted. */
    @Transactional(readOnly = true)
    public List<TaskEventResponse> history(Long ownerId, Long taskId) {
        List<TaskEvent> events = eventRepository.findByOwnerIdAndTaskIdOrderByOccurredAtAscIdAsc(ownerId, taskId);
        if (events.isEmpty()) {
            throw new TaskNotFoundException(taskId);
        }
        return events.stream().map(TaskEventResponse::from).toList();
    }

    private void applyStatus(Task task, Long ownerId, TaskStatus newStatus, Instant now) {
        // Setting the status a task already has is a no-op, not a new activity in the log
        if (task.getStatus() == newStatus) {
            return;
        }
        TaskStatus previous = task.changeStatus(newStatus, now);
        record(task, ownerId, TaskActivity.forStatusChange(previous, newStatus), previous, newStatus, now);
    }

    private void record(Task task, Long ownerId, TaskActivity activity, TaskStatus from, TaskStatus to, Instant now) {
        eventRepository.save(new TaskEvent(task.getId(), ownerId, activity, from, to, now));
    }

    // 404 rather than 403 for someone else's task, so ids of other users' tasks are not confirmed to exist
    private Task findOwnedTask(Long ownerId, Long taskId) {
        return taskRepository.findByIdAndOwnerId(taskId, ownerId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private static String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }
}

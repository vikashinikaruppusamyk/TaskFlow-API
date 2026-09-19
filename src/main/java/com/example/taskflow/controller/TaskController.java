package com.example.taskflow.controller;

import com.example.taskflow.dto.CreateTaskRequest;
import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.StatusChangeRequest;
import com.example.taskflow.dto.TaskEventResponse;
import com.example.taskflow.dto.TaskResponse;
import com.example.taskflow.dto.UpdateTaskRequest;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.security.AuthenticatedUser;
import com.example.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Manage your own tasks. Every change is recorded in the event log.")
@ApiResponse(responseCode = "401", description = "Missing, expired or invalid access token")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a task", description = "New tasks start in status TODO.")
    @ApiResponse(responseCode = "201", description = "Task created; the Location header points to it")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = taskService.create(user.id(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(task.id()).toUri();
        return ResponseEntity.created(location).body(task);
    }

    @GetMapping
    @Operation(summary = "List your tasks, newest first")
    @ApiResponse(responseCode = "200", description = "One page of tasks")
    @ApiResponse(responseCode = "400", description = "Invalid paging parameters or status")
    public PageResponse<TaskResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
                                           @RequestParam(required = false) TaskStatus status,
                                           @RequestParam(defaultValue = "0") @Min(0) int page,
                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return taskService.list(user.id(), status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of your tasks")
    @ApiResponse(responseCode = "200", description = "The task")
    @ApiResponse(responseCode = "404", description = "No task with this id belongs to you")
    public TaskResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return taskService.get(user.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a task's title, description and status")
    @ApiResponse(responseCode = "200", description = "Task updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "No task with this id belongs to you")
    @ApiResponse(responseCode = "409", description = "The task was modified concurrently")
    public TaskResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(user.id(), id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move a task to another status",
            description = "TODO -> IN_PROGRESS is logged as 'Start Work', anything -> COMPLETED as 'Complete Task', "
                    + "COMPLETED -> anything as 'Reopen Task', IN_PROGRESS -> TODO as 'Pause Work'.")
    @ApiResponse(responseCode = "200", description = "Status changed (or already had this status)")
    @ApiResponse(responseCode = "400", description = "Missing or unknown status")
    @ApiResponse(responseCode = "404", description = "No task with this id belongs to you")
    @ApiResponse(responseCode = "409", description = "The task was modified concurrently")
    public TaskResponse changeStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                     @Valid @RequestBody StatusChangeRequest request) {
        return taskService.changeStatus(user.id(), id, request.status());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task", description = "The task's event history is kept for analytics.")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "No task with this id belongs to you")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        taskService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Event history of a task (its trace), oldest first",
            description = "Also available for deleted tasks.")
    @ApiResponse(responseCode = "200", description = "The task's events")
    @ApiResponse(responseCode = "404", description = "No events for this task id belong to you")
    public List<TaskEventResponse> history(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return taskService.history(user.id(), id);
    }
}

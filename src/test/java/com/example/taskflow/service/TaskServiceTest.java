package com.example.taskflow.service;

import com.example.taskflow.MutableClock;
import com.example.taskflow.dto.UpdateTaskRequest;
import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskActivity;
import com.example.taskflow.entity.TaskEvent;
import com.example.taskflow.entity.TaskStatus;
import com.example.taskflow.entity.User;
import com.example.taskflow.exception.TaskNotFoundException;
import com.example.taskflow.repository.TaskEventRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final long OWNER_ID = 7L;
    private static final long TASK_ID = 3L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private TaskEventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    private final MutableClock clock = new MutableClock();
    private TaskService taskService;
    private Task task;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, eventRepository, userRepository, clock);
        task = new Task(new User("alex@example.com", "hash", MutableClock.START), "Write README", null, clock.instant());
    }

    private void taskExists() {
        when(taskRepository.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.of(task));
    }

    @Test
    void statusChangeIsRecordedWithTransitionAndTime() {
        taskExists();
        clock.advance(Duration.ofMinutes(10));

        taskService.changeStatus(OWNER_ID, TASK_ID, TaskStatus.IN_PROGRESS);

        ArgumentCaptor<TaskEvent> event = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getActivity()).isEqualTo(TaskActivity.STARTED);
        assertThat(event.getValue().getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(event.getValue().getFromStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(event.getValue().getToStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(event.getValue().getOccurredAt()).isEqualTo(clock.instant());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void settingTheSameStatusRecordsNothing() {
        taskExists();

        taskService.changeStatus(OWNER_ID, TASK_ID, TaskStatus.TODO);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void unchangedDetailsAreNotRecordedAsAnUpdate() {
        taskExists();

        taskService.update(OWNER_ID, TASK_ID, new UpdateTaskRequest("  Write README ", "", TaskStatus.COMPLETED));

        ArgumentCaptor<TaskEvent> event = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventRepository, times(1)).save(event.capture());
        assertThat(event.getValue().getActivity()).isEqualTo(TaskActivity.COMPLETED);
        assertThat(task.getCompletedAt()).isEqualTo(clock.instant());
    }

    @Test
    void deleteRecordsEventBeforeRemovingTask() {
        taskExists();

        taskService.delete(OWNER_ID, TASK_ID);

        ArgumentCaptor<TaskEvent> event = ArgumentCaptor.forClass(TaskEvent.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getActivity()).isEqualTo(TaskActivity.DELETED);
        verify(taskRepository).delete(task);
    }

    @Test
    void taskOfAnotherOwnerIsNotFound() {
        when(taskRepository.findByIdAndOwnerId(TASK_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.changeStatus(OWNER_ID, TASK_ID, TaskStatus.COMPLETED))
                .isInstanceOf(TaskNotFoundException.class);
        verify(eventRepository, never()).save(any());
    }
}

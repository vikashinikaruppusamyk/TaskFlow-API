package com.example.taskflow.repository;

import com.example.taskflow.entity.TaskEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskEventRepository extends JpaRepository<TaskEvent, Long> {

    List<TaskEvent> findByOwnerIdOrderByTaskIdAscOccurredAtAscIdAsc(Long ownerId);

    List<TaskEvent> findByOwnerIdAndTaskIdOrderByOccurredAtAscIdAsc(Long ownerId, Long taskId);
}

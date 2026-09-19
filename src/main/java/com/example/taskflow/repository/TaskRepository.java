package com.example.taskflow.repository;

import com.example.taskflow.entity.Task;
import com.example.taskflow.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Every lookup is scoped to an owner, so one user can never read or change another user's tasks. */
public interface TaskRepository extends JpaRepository<Task, Long> {

    Optional<Task> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Task> findByOwnerId(Long ownerId, Pageable pageable);

    Page<Task> findByOwnerIdAndStatus(Long ownerId, TaskStatus status, Pageable pageable);

    @Query("select t.status as status, count(t) as count from Task t where t.owner.id = :ownerId group by t.status")
    List<StatusCount> countByStatus(Long ownerId);

    interface StatusCount {
        TaskStatus getStatus();

        long getCount();
    }
}

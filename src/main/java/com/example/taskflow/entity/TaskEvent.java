package com.example.taskflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row of the event log: which activity happened to which case (task), and when.
 * Events are append-only and are kept after their task is deleted.
 */
@Entity
@Table(name = "task_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskActivity activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private TaskStatus toStatus;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public TaskEvent(Long taskId, Long ownerId, TaskActivity activity,
                     TaskStatus fromStatus, TaskStatus toStatus, Instant occurredAt) {
        this.taskId = taskId;
        this.ownerId = ownerId;
        this.activity = activity;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.occurredAt = occurredAt;
    }
}

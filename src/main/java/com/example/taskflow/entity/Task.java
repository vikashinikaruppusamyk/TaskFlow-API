package com.example.taskflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Optimistic locking: two concurrent updates of the same task cannot silently overwrite each other
    @Version
    private long version;

    public Task(User owner, String title, String description, Instant now) {
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.TODO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Returns true if the title or description actually changed. */
    public boolean updateDetails(String title, String description, Instant now) {
        if (Objects.equals(this.title, title) && Objects.equals(this.description, description)) {
            return false;
        }
        this.title = title;
        this.description = description;
        this.updatedAt = now;
        return true;
    }

    /** Moves the task to a new status and returns the previous one. */
    public TaskStatus changeStatus(TaskStatus newStatus, Instant now) {
        TaskStatus previous = this.status;
        this.status = newStatus;
        this.completedAt = newStatus == TaskStatus.COMPLETED ? now : null;
        this.updatedAt = now;
        return previous;
    }
}

package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.TaskPriority;
import com.trishakti.crm.domain.enums.TaskStatus;
import com.trishakti.crm.domain.enums.TaskType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "task", indexes = {
        @Index(name = "idx_task_assignee_status", columnList = "assignee_id,status"),
        @Index(name = "idx_task_due", columnList = "due_at"),
        @Index(name = "idx_task_reminder", columnList = "reminder_at,notified")
})
public class Task extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TaskType type = TaskType.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TaskStatus status = TaskStatus.OPEN;

    @Column(name = "reminder_at")
    private Instant reminderAt;

    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "completed_at")
    private Instant completedAt;
}

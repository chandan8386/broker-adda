package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.TaskPriority;
import com.trishakti.crm.domain.enums.TaskStatus;
import com.trishakti.crm.domain.enums.TaskType;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class TaskDtos {
    private TaskDtos() {}

    public record TaskRequest(
            @NotBlank String title,
            String description,
            TaskType type,
            Long assigneeId,
            Long leadId,
            Instant dueAt,
            Instant reminderAt,
            TaskPriority priority) {}

    public record UpdateTaskRequest(
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Instant dueAt,
            Instant reminderAt) {}

    public record TaskResponse(
            Long id, String title, String description, TaskType type,
            Long assigneeId, String assigneeName, Long leadId, String leadReference,
            Instant dueAt, Instant reminderAt, TaskPriority priority, TaskStatus status,
            Instant completedAt, Instant createdAt) {}
}

package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.NotificationType;

import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(
            Long id, NotificationType type, String title, String body,
            String entityType, Long entityId, boolean read, Instant readAt, Instant createdAt) {}

    public record UnreadCountResponse(long unread) {}
}

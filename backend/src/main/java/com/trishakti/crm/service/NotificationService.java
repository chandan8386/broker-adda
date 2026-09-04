package com.trishakti.crm.service;

import com.trishakti.crm.domain.Notification;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.NotificationType;
import com.trishakti.crm.dto.NotificationDtos.NotificationResponse;
import com.trishakti.crm.dto.NotificationDtos.UnreadCountResponse;
import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.NotificationRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification push(User recipient, NotificationType type, String title, String body,
                             String entityType, Long entityId) {
        if (recipient == null) return null;
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setEntityType(entityType);
        n.setEntityId(entityId);
        return notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, boolean unreadOnly, Pageable pageable) {
        var page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadFalseOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(page, CrmMappers::notification);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByRecipientIdAndReadFalse(userId));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getRecipient().getId().equals(userId))
                .ifPresent(n -> {
                    n.setRead(true);
                    n.setReadAt(Instant.now());
                });
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, Instant.now());
    }
}

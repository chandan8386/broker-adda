package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.NotificationType;
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
@Table(name = "notification", indexes = {
        @Index(name = "idx_notif_recipient_read", columnList = "recipient_id,is_read"),
        @Index(name = "idx_notif_created", columnList = "created_at")
})
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 1000)
    private String body;

    @Column(name = "entity_type", length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;
}

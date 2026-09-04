package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.FollowUpChannel;
import com.trishakti.crm.domain.enums.FollowUpStatus;
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
@Table(name = "follow_up", indexes = {
        @Index(name = "idx_followup_due", columnList = "due_at"),
        @Index(name = "idx_followup_owner_status", columnList = "owner_id,status")
})
public class FollowUp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private FollowUpChannel channel = FollowUpChannel.CALL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private FollowUpStatus status = FollowUpStatus.PENDING;

    @Column(length = 1000)
    private String notes;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(nullable = false)
    private boolean notified = false;
}

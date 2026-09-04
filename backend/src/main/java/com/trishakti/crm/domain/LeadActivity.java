package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.LeadStatus;
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
@Table(name = "lead_activity", indexes = @Index(name = "idx_activity_lead_time", columnList = "lead_id,occurred_at"))
public class LeadActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private LeadStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private LeadStatus toStatus;

    @Column(nullable = false, length = 255)
    private String summary;

    @Column(length = 4000)
    private String detail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}

package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.CallDirection;
import com.trishakti.crm.domain.enums.CallDisposition;
import com.trishakti.crm.domain.enums.CallOutcome;
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
@Table(name = "call_log", indexes = {
        @Index(name = "idx_call_lead_time", columnList = "lead_id,called_at"),
        @Index(name = "idx_call_caller_time", columnList = "caller_id,called_at")
})
public class CallLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caller_id")
    private User caller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private CallDirection direction = CallDirection.OUTBOUND;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    private CallDisposition disposition;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 2000)
    private String notes;

    @Column(name = "called_at", nullable = false)
    private Instant calledAt = Instant.now();

    @Column(name = "next_follow_up_at")
    private Instant nextFollowUpAt;
}

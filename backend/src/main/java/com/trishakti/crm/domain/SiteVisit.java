package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.SiteVisitResult;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
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
@Table(name = "site_visit", indexes = {
        @Index(name = "idx_sitevisit_scheduled", columnList = "scheduled_at"),
        @Index(name = "idx_sitevisit_exec_status", columnList = "sales_executive_id,status")
})
public class SiteVisit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_executive_id")
    private User salesExecutive;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SiteVisitStatus status = SiteVisitStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SiteVisitResult result;

    @Column(length = 2000)
    private String feedback;

    @Column(name = "pickup_required", nullable = false)
    private boolean pickupRequired = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    private Integer rating;
}

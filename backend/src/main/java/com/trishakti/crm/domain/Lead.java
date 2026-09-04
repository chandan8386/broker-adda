package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
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

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_lead_status", columnList = "status"),
        @Index(name = "idx_lead_assignee_status", columnList = "assigned_user_id,status"),
        @Index(name = "idx_lead_follow_up", columnList = "next_follow_up_at"),
        @Index(name = "idx_lead_mobile", columnList = "mobile"),
        @Index(name = "idx_lead_source_channel", columnList = "source_channel"),
        @Index(name = "idx_lead_created_at", columnList = "created_at")
})
public class Lead extends BaseEntity {

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(length = 150)
    private String email;

    @Column(length = 120)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_channel", length = 30)
    private SourceChannel sourceChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", length = 30)
    private PropertyType propertyType;

    @Column(name = "budget_min", precision = 14, scale = 2)
    private BigDecimal budgetMin;

    @Column(name = "budget_max", precision = 14, scale = 2)
    private BigDecimal budgetMax;

    @Column(name = "preferred_location", length = 150)
    private String preferredLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeadStatus status = LeadStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LeadPriority priority = LeadPriority.WARM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interested_property_id")
    private Property interestedProperty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_customer_id")
    private Customer convertedCustomer;

    @Column(name = "next_follow_up_at")
    private Instant nextFollowUpAt;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Column(length = 4000)
    private String notes;

    @Column(name = "lost_reason", length = 255)
    private String lostReason;

    @Column(nullable = false)
    private boolean deleted = false;

    public String getReference() {
        return "LD-" + (getId() == null ? "NEW" : String.format("%06d", getId()));
    }
}

package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "booking",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_number", columnNames = "booking_number"),
        indexes = {
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_exec", columnList = "sales_executive_id")
        })
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_visit_id")
    private SiteVisit siteVisit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_executive_id")
    private User salesExecutive;

    @Column(name = "booking_number", nullable = false, length = 40)
    private String bookingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BookingStatus status = BookingStatus.NEGOTIATION;

    @Column(name = "quoted_price", precision = 14, scale = 2)
    private BigDecimal quotedPrice;

    @Column(name = "negotiated_price", precision = 14, scale = 2)
    private BigDecimal negotiatedPrice;

    @Column(precision = 14, scale = 2)
    private BigDecimal discount;

    @Column(name = "token_amount", precision = 14, scale = 2)
    private BigDecimal tokenAmount;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(name = "expected_closure_date")
    private LocalDate expectedClosureDate;

    @Column(length = 2000)
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;
}

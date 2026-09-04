package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.PurchaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "purchase",
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_booking", columnNames = "booking_id"))
public class Purchase extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "sale_deed_number", length = 60)
    private String saleDeedNumber;

    @Column(name = "final_price", precision = 14, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "total_paid", precision = 14, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PurchaseStatus status = PurchaseStatus.IN_PROGRESS;

    @Column(name = "agreement_date")
    private LocalDate agreementDate;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "possession_date")
    private LocalDate possessionDate;
}

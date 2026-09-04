package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.PaymentMode;
import com.trishakti.crm.domain.enums.PaymentStatus;
import com.trishakti.crm.domain.enums.PaymentType;
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
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_payment_booking", columnList = "booking_id"),
        @Index(name = "idx_payment_purchase", columnList = "purchase_id"),
        @Index(name = "idx_payment_status_due", columnList = "status,due_date")
})
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMode mode;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PaymentStatus status = PaymentStatus.DUE;

    @Column(name = "reference_number", length = 80)
    private String referenceNumber;

    @Column(length = 500)
    private String remarks;
}

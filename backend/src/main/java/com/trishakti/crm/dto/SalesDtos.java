package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.BookingStatus;
import com.trishakti.crm.domain.enums.PaymentMode;
import com.trishakti.crm.domain.enums.PaymentStatus;
import com.trishakti.crm.domain.enums.PaymentType;
import com.trishakti.crm.domain.enums.PurchaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SalesDtos {
    private SalesDtos() {}

    public record CreateBookingRequest(
            @NotNull Long leadId,
            Long siteVisitId,
            @NotNull Long propertyId,
            Long customerId,
            @Positive BigDecimal quotedPrice,
            BigDecimal negotiatedPrice,
            BigDecimal discount,
            BigDecimal tokenAmount,
            LocalDate bookingDate,
            LocalDate expectedClosureDate,
            String notes) {}

    public record UpdateBookingRequest(
            BookingStatus status,
            BigDecimal negotiatedPrice,
            BigDecimal discount,
            BigDecimal tokenAmount,
            LocalDate expectedClosureDate,
            String notes,
            String cancelReason) {}

    public record BookingResponse(
            Long id, String bookingNumber, Long leadId, String leadName,
            Long propertyId, String propertyTitle, Long customerId, String customerName,
            Long salesExecutiveId, String salesExecutiveName, BookingStatus status,
            BigDecimal quotedPrice, BigDecimal negotiatedPrice, BigDecimal discount, BigDecimal tokenAmount,
            LocalDate bookingDate, LocalDate expectedClosureDate, String notes, String cancelReason,
            List<PaymentResponse> payments) {}

    public record CreatePurchaseRequest(
            @NotNull Long bookingId,
            @Positive BigDecimal finalPrice,
            String saleDeedNumber,
            LocalDate agreementDate) {}

    public record UpdatePurchaseRequest(
            PurchaseStatus status,
            BigDecimal finalPrice,
            String saleDeedNumber,
            LocalDate agreementDate,
            LocalDate registrationDate,
            LocalDate possessionDate) {}

    public record PurchaseResponse(
            Long id, Long bookingId, String bookingNumber, Long propertyId, String propertyTitle,
            Long customerId, String customerName, String saleDeedNumber,
            BigDecimal finalPrice, BigDecimal totalPaid, BigDecimal balance, PurchaseStatus status,
            LocalDate agreementDate, LocalDate registrationDate, LocalDate possessionDate,
            List<PaymentResponse> payments) {}

    public record RecordPaymentRequest(
            Long bookingId,
            Long purchaseId,
            @NotNull PaymentType type,
            PaymentMode mode,
            @NotNull @Positive BigDecimal amount,
            LocalDate dueDate,
            LocalDate paidDate,
            PaymentStatus status,
            String referenceNumber,
            String remarks) {}

    public record PaymentResponse(
            Long id, Long bookingId, Long purchaseId, PaymentType type, PaymentMode mode,
            BigDecimal amount, LocalDate dueDate, LocalDate paidDate, PaymentStatus status,
            String referenceNumber, String remarks) {}
}

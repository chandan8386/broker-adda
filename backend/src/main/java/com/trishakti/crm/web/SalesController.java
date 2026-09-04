package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.enums.BookingStatus;
import com.trishakti.crm.dto.SalesDtos.*;
import com.trishakti.crm.service.SalesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/sales")
@Tag(name = "Sales / Purchase Management")
@PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER','SALES_EXECUTIVE')")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    // ---- Bookings ----

    @PostMapping("/bookings")
    @Operation(summary = "Create a booking from an interested lead + property")
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return salesService.createBooking(request);
    }

    @PutMapping("/bookings/{id}")
    @Operation(summary = "Update negotiation / booking details / status")
    public BookingResponse updateBooking(@PathVariable Long id, @Valid @RequestBody UpdateBookingRequest request) {
        return salesService.updateBooking(id, request);
    }

    @GetMapping("/bookings/{id}")
    public BookingResponse getBooking(@PathVariable Long id) {
        return salesService.getBooking(id);
    }

    @GetMapping("/bookings")
    public PageResponse<BookingResponse> listBookings(@RequestParam(required = false) BookingStatus status,
                                                     @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return salesService.listBookings(status, pageable);
    }

    // ---- Purchases ----

    @PostMapping("/purchases")
    @Operation(summary = "Convert a booking into a purchase")
    public PurchaseResponse createPurchase(@Valid @RequestBody CreatePurchaseRequest request) {
        return salesService.createPurchase(request);
    }

    @PutMapping("/purchases/{id}")
    public PurchaseResponse updatePurchase(@PathVariable Long id, @Valid @RequestBody UpdatePurchaseRequest request) {
        return salesService.updatePurchase(id, request);
    }

    @GetMapping("/purchases/{id}")
    public PurchaseResponse getPurchase(@PathVariable Long id) {
        return salesService.getPurchase(id);
    }

    @GetMapping("/purchases")
    public PageResponse<PurchaseResponse> listPurchases(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return salesService.listPurchases(pageable);
    }

    // ---- Payments ----

    @PostMapping("/payments")
    @Operation(summary = "Record a payment / instalment against a booking or purchase")
    public PaymentResponse recordPayment(@Valid @RequestBody RecordPaymentRequest request) {
        return salesService.recordPayment(request);
    }

    @PostMapping("/payments/{id}/mark-paid")
    public PaymentResponse markPaid(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        LocalDate paidDate = body != null && body.get("paidDate") != null ? LocalDate.parse(body.get("paidDate")) : null;
        String ref = body != null ? body.get("referenceNumber") : null;
        return salesService.markPaid(id, paidDate, ref);
    }
}

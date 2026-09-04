package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.*;
import com.trishakti.crm.domain.enums.*;
import com.trishakti.crm.dto.SalesDtos.*;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.*;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class SalesService {

    private final BookingRepository bookingRepository;
    private final PurchaseRepository purchaseRepository;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final LeadService leadService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public SalesService(BookingRepository bookingRepository, PurchaseRepository purchaseRepository,
                        PaymentRepository paymentRepository, PropertyRepository propertyRepository,
                        CustomerRepository customerRepository, UserRepository userRepository,
                        LeadService leadService, NotificationService notificationService,
                        AuditService auditService) {
        this.bookingRepository = bookingRepository;
        this.purchaseRepository = purchaseRepository;
        this.paymentRepository = paymentRepository;
        this.propertyRepository = propertyRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.leadService = leadService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ---------------- Booking ----------------

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest req) {
        Lead lead = leadService.loadVisible(req.leadId());
        Property property = propertyRepository.findById(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", req.propertyId()));
        if (property.getStatus() == PropertyStatus.SOLD) {
            throw new BusinessRuleException("Property is already sold");
        }
        User exec = currentUser();

        Customer customer = resolveCustomer(req.customerId(), lead);

        Booking booking = new Booking();
        booking.setLead(lead);
        booking.setProperty(property);
        booking.setCustomer(customer);
        booking.setSalesExecutive(exec);
        booking.setBookingNumber(nextBookingNumber());
        booking.setStatus(BookingStatus.TENTATIVE);
        booking.setQuotedPrice(req.quotedPrice() != null ? req.quotedPrice() : property.getPrice());
        booking.setNegotiatedPrice(req.negotiatedPrice());
        booking.setDiscount(req.discount());
        booking.setTokenAmount(req.tokenAmount());
        booking.setBookingDate(req.bookingDate() != null ? req.bookingDate() : LocalDate.now());
        booking.setExpectedClosureDate(req.expectedClosureDate());
        booking.setNotes(req.notes());
        if (req.siteVisitId() != null) {
            // link if present; light lookup avoided for brevity
        }
        bookingRepository.save(booking);

        property.setStatus(PropertyStatus.HELD);
        lead.setConvertedCustomer(customer);
        moveLead(lead, LeadStatus.BOOKING);

        if (req.tokenAmount() != null && req.tokenAmount().signum() > 0) {
            recordPaymentInternal(booking, null, PaymentType.TOKEN, PaymentMode.UPI, req.tokenAmount(),
                    LocalDate.now(), LocalDate.now(), PaymentStatus.PAID, null, "Token amount at booking");
        }

        leadService.logActivity(lead, ActivityType.BOOKING, lead.getStatus(), LeadStatus.BOOKING,
                "Booking " + booking.getBookingNumber() + " created for " + property.getTitle(), null, exec);
        notifyManagers(NotificationType.BOOKING_CREATED, "New booking " + booking.getBookingNumber(),
                lead.getCustomerName() + " booked " + property.getTitle(), "Booking", booking.getId());
        auditService.record("BOOKING_CREATED", "Booking", booking.getId(), null, null);

        return CrmMappers.booking(booking, paymentRepository.findByBookingIdOrderByDueDateAsc(booking.getId()));
    }

    @Transactional
    public BookingResponse updateBooking(Long id, UpdateBookingRequest req) {
        Booking booking = bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        if (req.negotiatedPrice() != null) booking.setNegotiatedPrice(req.negotiatedPrice());
        if (req.discount() != null) booking.setDiscount(req.discount());
        if (req.tokenAmount() != null) booking.setTokenAmount(req.tokenAmount());
        if (req.expectedClosureDate() != null) booking.setExpectedClosureDate(req.expectedClosureDate());
        if (req.notes() != null) booking.setNotes(req.notes());
        if (req.status() != null) {
            booking.setStatus(req.status());
            if (req.status() == BookingStatus.CANCELLED) {
                booking.setCancelReason(req.cancelReason());
                booking.getProperty().setStatus(PropertyStatus.AVAILABLE);
                moveLead(booking.getLead(), LeadStatus.NEGOTIATION);
            } else if (req.status() == BookingStatus.CONFIRMED) {
                booking.getProperty().setStatus(PropertyStatus.BOOKED);
            }
        }
        auditService.record("BOOKING_UPDATED", "Booking", id, null, null);
        return CrmMappers.booking(booking, paymentRepository.findByBookingIdOrderByDueDateAsc(id));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id) {
        Booking booking = bookingRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
        return CrmMappers.booking(booking, paymentRepository.findByBookingIdOrderByDueDateAsc(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listBookings(BookingStatus status, Pageable pageable) {
        var page = status != null
                ? bookingRepository.findByStatus(status, pageable)
                : bookingRepository.findAll(pageable);
        return PageResponse.of(page, b -> CrmMappers.booking(b,
                paymentRepository.findByBookingIdOrderByDueDateAsc(b.getId())));
    }

    // ---------------- Purchase ----------------

    @Transactional
    public PurchaseResponse createPurchase(CreatePurchaseRequest req) {
        Booking booking = bookingRepository.findWithDetailsById(req.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", req.bookingId()));
        if (purchaseRepository.findByBookingId(booking.getId()).isPresent()) {
            throw new BusinessRuleException("A purchase already exists for this booking");
        }
        booking.setStatus(BookingStatus.CONVERTED);

        Purchase purchase = new Purchase();
        purchase.setBooking(booking);
        purchase.setProperty(booking.getProperty());
        purchase.setCustomer(booking.getCustomer());
        purchase.setSaleDeedNumber(req.saleDeedNumber());
        purchase.setFinalPrice(req.finalPrice() != null ? req.finalPrice()
                : (booking.getNegotiatedPrice() != null ? booking.getNegotiatedPrice() : booking.getQuotedPrice()));
        purchase.setAgreementDate(req.agreementDate());
        purchase.setStatus(PurchaseStatus.IN_PROGRESS);
        BigDecimal paid = totalPaidForBooking(booking.getId());
        purchase.setTotalPaid(paid);
        purchase.setBalance(purchase.getFinalPrice().subtract(paid));
        purchaseRepository.save(purchase);

        booking.getProperty().setStatus(PropertyStatus.SOLD);
        Lead lead = booking.getLead();
        moveLead(lead, LeadStatus.PURCHASED);
        leadService.logActivity(lead, ActivityType.STATUS_CHANGE, LeadStatus.BOOKING, LeadStatus.PURCHASED,
                "Purchase completed for " + booking.getProperty().getTitle(), null, currentUser());
        auditService.record("PURCHASE_CREATED", "Purchase", purchase.getId(), null, null);

        return CrmMappers.purchase(purchase, paymentRepository.findByBookingIdOrderByDueDateAsc(booking.getId()));
    }

    @Transactional
    public PurchaseResponse updatePurchase(Long id, UpdatePurchaseRequest req) {
        Purchase purchase = purchaseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
        if (req.status() != null) purchase.setStatus(req.status());
        if (req.finalPrice() != null) purchase.setFinalPrice(req.finalPrice());
        if (req.saleDeedNumber() != null) purchase.setSaleDeedNumber(req.saleDeedNumber());
        if (req.agreementDate() != null) purchase.setAgreementDate(req.agreementDate());
        if (req.registrationDate() != null) purchase.setRegistrationDate(req.registrationDate());
        if (req.possessionDate() != null) purchase.setPossessionDate(req.possessionDate());
        recomputePurchaseBalance(purchase);
        auditService.record("PURCHASE_UPDATED", "Purchase", id, null, null);
        return CrmMappers.purchase(purchase, allPaymentsForPurchase(purchase));
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getPurchase(Long id) {
        Purchase purchase = purchaseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase", id));
        return CrmMappers.purchase(purchase, allPaymentsForPurchase(purchase));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseResponse> listPurchases(Pageable pageable) {
        return PageResponse.of(purchaseRepository.findAllBy(pageable),
                p -> CrmMappers.purchase(p, allPaymentsForPurchase(p)));
    }

    // ---------------- Payment ----------------

    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest req) {
        Booking booking = null;
        Purchase purchase = null;
        if (req.bookingId() != null) {
            booking = bookingRepository.findById(req.bookingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Booking", req.bookingId()));
        }
        if (req.purchaseId() != null) {
            purchase = purchaseRepository.findById(req.purchaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase", req.purchaseId()));
        }
        if (booking == null && purchase == null) {
            throw new BusinessRuleException("A payment must be linked to a booking or a purchase");
        }
        Payment payment = recordPaymentInternal(booking, purchase, req.type(), req.mode(), req.amount(),
                req.dueDate(), req.paidDate(),
                req.status() != null ? req.status() : (req.paidDate() != null ? PaymentStatus.PAID : PaymentStatus.DUE),
                req.referenceNumber(), req.remarks());

        if (purchase != null) recomputePurchaseBalance(purchase);
        auditService.record("PAYMENT_RECORDED", "Payment", payment.getId(), null, null);
        notifyManagers(NotificationType.PAYMENT_RECEIVED, "Payment recorded",
                req.amount() + " (" + req.type() + ")", "Payment", payment.getId());
        return CrmMappers.payment(payment);
    }

    @Transactional
    public PaymentResponse markPaid(Long paymentId, LocalDate paidDate, String reference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(paidDate != null ? paidDate : LocalDate.now());
        if (reference != null) payment.setReferenceNumber(reference);
        if (payment.getPurchase() != null) recomputePurchaseBalance(payment.getPurchase());
        return CrmMappers.payment(payment);
    }

    // ---------------- helpers ----------------

    private Payment recordPaymentInternal(Booking booking, Purchase purchase, PaymentType type, PaymentMode mode,
                                          BigDecimal amount, LocalDate dueDate, LocalDate paidDate,
                                          PaymentStatus status, String reference, String remarks) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPurchase(purchase);
        payment.setType(type);
        payment.setMode(mode);
        payment.setAmount(amount);
        payment.setDueDate(dueDate);
        payment.setPaidDate(paidDate);
        payment.setStatus(status);
        payment.setReferenceNumber(reference);
        payment.setRemarks(remarks);
        return paymentRepository.save(payment);
    }

    private void recomputePurchaseBalance(Purchase purchase) {
        BigDecimal paid = allPaymentsForPurchase(purchase).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        purchase.setTotalPaid(paid);
        if (purchase.getFinalPrice() != null) {
            purchase.setBalance(purchase.getFinalPrice().subtract(paid));
            if (purchase.getBalance().signum() <= 0 && purchase.getStatus() == PurchaseStatus.IN_PROGRESS) {
                purchase.setStatus(PurchaseStatus.COMPLETED);
            }
        }
    }

    private List<Payment> allPaymentsForPurchase(Purchase purchase) {
        List<Payment> direct = paymentRepository.findByPurchaseIdOrderByDueDateAsc(purchase.getId());
        List<Payment> viaBooking = paymentRepository.findByBookingIdOrderByDueDateAsc(purchase.getBooking().getId());
        viaBooking.removeIf(direct::contains);
        direct.addAll(viaBooking);
        return direct;
    }

    private BigDecimal totalPaidForBooking(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByDueDateAsc(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Customer resolveCustomer(Long customerId, Lead lead) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        }
        return customerRepository.findByMobile(lead.getMobile()).orElseGet(() -> {
            Customer c = new Customer();
            c.setFullName(lead.getCustomerName());
            c.setMobile(lead.getMobile());
            c.setEmail(lead.getEmail());
            c.setCity(lead.getPreferredLocation());
            c.setSourceLeadId(lead.getId());
            return customerRepository.save(c);
        });
    }

    private String nextBookingNumber() {
        String prefix = "STIR/BK/" + Year.now().getValue() + "/";
        long seq = bookingRepository.count() + 1;
        String candidate;
        do {
            candidate = prefix + String.format("%05d", seq++);
        } while (bookingRepository.existsByBookingNumber(candidate));
        return candidate;
    }

    private void moveLead(Lead lead, LeadStatus target) {
        if (lead.getStatus() != target && lead.getStatus().canTransitionTo(target)) {
            lead.setStatus(target);
        }
    }

    private void notifyManagers(NotificationType type, String title, String body, String entityType, Long entityId) {
        userRepository.findActiveByRole(RoleName.SALES_MANAGER)
                .forEach(m -> notificationService.push(m, type, title, body, entityType, entityId));
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }
}

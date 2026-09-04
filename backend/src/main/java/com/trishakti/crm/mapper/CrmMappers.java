package com.trishakti.crm.mapper;

import com.trishakti.crm.domain.*;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.dto.*;

import java.util.Set;
import java.util.stream.Collectors;

/** Plain entity -> DTO mappers. Kept explicit and dependency-free. */
public final class CrmMappers {

    private CrmMappers() {}

    // ---------- User / Team ----------

    public static Set<RoleName> roleNames(User u) {
        return u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    public static AuthDtos.UserSummary userSummary(User u) {
        return new AuthDtos.UserSummary(u.getId(), u.getFullName(), u.getUsername(), u.getEmail(),
                u.getPhone(),
                u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                u.getTeam() != null ? u.getTeam().getName() : null);
    }

    public static UserDtos.UserResponse user(User u) {
        return new UserDtos.UserResponse(
                u.getId(), u.getFullName(), u.getUsername(), u.getEmail(), u.getPhone(),
                u.getTeam() != null ? u.getTeam().getId() : null,
                u.getTeam() != null ? u.getTeam().getName() : null,
                u.getManager() != null ? u.getManager().getId() : null,
                u.getManager() != null ? u.getManager().getFullName() : null,
                roleNames(u), u.isActive(), u.getLastLoginAt(), u.getCreatedAt());
    }

    public static UserDtos.TeamResponse team(Team t, int memberCount) {
        return new UserDtos.TeamResponse(t.getId(), t.getName(), t.getDescription(),
                t.getManager() != null ? t.getManager().getId() : null,
                t.getManager() != null ? t.getManager().getFullName() : null,
                t.isActive(), memberCount);
    }

    // ---------- Lead ----------

    public static LeadDtos.LeadResponse lead(Lead l) {
        return new LeadDtos.LeadResponse(
                l.getId(), l.getReference(), l.getCustomerName(), l.getMobile(), l.getEmail(),
                l.getSource(), l.getSourceChannel(), l.getPropertyType(),
                l.getBudgetMin(), l.getBudgetMax(), l.getPreferredLocation(),
                l.getStatus(), l.getPriority(),
                l.getAssignedUser() != null ? l.getAssignedUser().getId() : null,
                l.getAssignedUser() != null ? l.getAssignedUser().getFullName() : null,
                l.getInterestedProperty() != null ? l.getInterestedProperty().getId() : null,
                l.getInterestedProperty() != null ? l.getInterestedProperty().getTitle() : null,
                l.getConvertedCustomer() != null ? l.getConvertedCustomer().getId() : null,
                l.getNextFollowUpAt(), l.getLastContactedAt(),
                l.getNotes(), l.getLostReason(),
                l.getCreatedAt(), l.getUpdatedAt(), l.getCreatedBy());
    }

    public static LeadDtos.LeadListItem leadListItem(Lead l) {
        return new LeadDtos.LeadListItem(
                l.getId(), l.getReference(), l.getCustomerName(), l.getMobile(),
                l.getSourceChannel(), l.getPropertyType(), l.getStatus(), l.getPriority(),
                l.getAssignedUser() != null ? l.getAssignedUser().getFullName() : null,
                l.getNextFollowUpAt(), l.getCreatedAt());
    }

    public static LeadDtos.ActivityResponse activity(LeadActivity a) {
        return new LeadDtos.ActivityResponse(
                a.getId(), a.getType(), a.getFromStatus(), a.getToStatus(),
                a.getSummary(), a.getDetail(),
                a.getActor() != null ? a.getActor().getFullName() : "System",
                a.getOccurredAt());
    }

    public static AssignmentDtos.AssignmentHistoryResponse assignment(LeadAssignment a) {
        return new AssignmentDtos.AssignmentHistoryResponse(
                a.getId(), a.getLead().getId(),
                a.getFromUser() != null ? a.getFromUser().getId() : null,
                a.getFromUser() != null ? a.getFromUser().getFullName() : null,
                a.getToUser() != null ? a.getToUser().getId() : null,
                a.getToUser() != null ? a.getToUser().getFullName() : null,
                a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : null,
                a.getReason(), a.getAssignedAt());
    }

    // ---------- Call / Follow-up ----------

    public static CallDtos.CallResponse call(CallLog c) {
        return new CallDtos.CallResponse(
                c.getId(), c.getLead().getId(), c.getLead().getCustomerName(),
                c.getCaller() != null ? c.getCaller().getId() : null,
                c.getCaller() != null ? c.getCaller().getFullName() : null,
                c.getDirection(), c.getOutcome(), c.getDisposition(),
                c.getDurationSeconds(), c.getNotes(), c.getCalledAt(), c.getNextFollowUpAt());
    }

    public static CallDtos.FollowUpResponse followUp(FollowUp f) {
        return new CallDtos.FollowUpResponse(
                f.getId(), f.getLead().getId(), f.getLead().getCustomerName(),
                f.getOwner() != null ? f.getOwner().getId() : null,
                f.getOwner() != null ? f.getOwner().getFullName() : null,
                f.getDueAt(), f.getChannel().name(), f.getStatus().name(), f.getNotes(), f.getCompletedAt());
    }

    // ---------- Site visit ----------

    public static SiteVisitDtos.SiteVisitResponse siteVisit(SiteVisit s) {
        return new SiteVisitDtos.SiteVisitResponse(
                s.getId(), s.getLead().getId(), s.getLead().getCustomerName(), s.getLead().getMobile(),
                s.getProperty() != null ? s.getProperty().getId() : null,
                s.getProperty() != null ? s.getProperty().getTitle() : null,
                s.getSalesExecutive() != null ? s.getSalesExecutive().getId() : null,
                s.getSalesExecutive() != null ? s.getSalesExecutive().getFullName() : null,
                s.getScheduledAt(), s.getStatus(), s.getResult(), s.getFeedback(),
                s.isPickupRequired(), s.getRating(), s.getCompletedAt(), s.getCreatedAt());
    }

    // ---------- Property / Customer ----------

    public static PropertyDtos.PropertyResponse property(Property p) {
        return new PropertyDtos.PropertyResponse(
                p.getId(),
                p.getProject() != null ? p.getProject().getId() : null,
                p.getProject() != null ? p.getProject().getName() : null,
                p.getTitle(), p.getPropertyType(), p.getUnitNumber(), p.getLocation(), p.getCity(),
                p.getAreaSqft(), p.getPrice(), p.getBedrooms(), p.getBathrooms(), p.getFacing(),
                p.getStatus(), p.getDescription());
    }

    public static PropertyDtos.ProjectResponse project(PropertyProject p) {
        return new PropertyDtos.ProjectResponse(p.getId(), p.getName(), p.getLocation(),
                p.getCity(), p.getDescription(), p.isActive());
    }

    public static CustomerDtos.CustomerResponse customer(Customer c) {
        return new CustomerDtos.CustomerResponse(
                c.getId(), c.getFullName(), c.getMobile(), c.getAltMobile(), c.getEmail(),
                c.getAddress(), c.getCity(), c.getIdProofType(), c.getIdProofNumber(),
                c.getNotes(), c.getSourceLeadId(), c.getCreatedAt());
    }

    // ---------- Sales ----------

    public static SalesDtos.PaymentResponse payment(Payment p) {
        return new SalesDtos.PaymentResponse(
                p.getId(),
                p.getBooking() != null ? p.getBooking().getId() : null,
                p.getPurchase() != null ? p.getPurchase().getId() : null,
                p.getType(), p.getMode(), p.getAmount(), p.getDueDate(), p.getPaidDate(),
                p.getStatus(), p.getReferenceNumber(), p.getRemarks());
    }

    public static SalesDtos.BookingResponse booking(Booking b, java.util.List<Payment> payments) {
        return new SalesDtos.BookingResponse(
                b.getId(), b.getBookingNumber(), b.getLead().getId(), b.getLead().getCustomerName(),
                b.getProperty().getId(), b.getProperty().getTitle(),
                b.getCustomer() != null ? b.getCustomer().getId() : null,
                b.getCustomer() != null ? b.getCustomer().getFullName() : null,
                b.getSalesExecutive() != null ? b.getSalesExecutive().getId() : null,
                b.getSalesExecutive() != null ? b.getSalesExecutive().getFullName() : null,
                b.getStatus(), b.getQuotedPrice(), b.getNegotiatedPrice(), b.getDiscount(), b.getTokenAmount(),
                b.getBookingDate(), b.getExpectedClosureDate(), b.getNotes(), b.getCancelReason(),
                payments.stream().map(CrmMappers::payment).toList());
    }

    public static SalesDtos.PurchaseResponse purchase(Purchase p, java.util.List<Payment> payments) {
        return new SalesDtos.PurchaseResponse(
                p.getId(), p.getBooking().getId(), p.getBooking().getBookingNumber(),
                p.getProperty().getId(), p.getProperty().getTitle(),
                p.getCustomer() != null ? p.getCustomer().getId() : null,
                p.getCustomer() != null ? p.getCustomer().getFullName() : null,
                p.getSaleDeedNumber(), p.getFinalPrice(), p.getTotalPaid(), p.getBalance(), p.getStatus(),
                p.getAgreementDate(), p.getRegistrationDate(), p.getPossessionDate(),
                payments.stream().map(CrmMappers::payment).toList());
    }

    // ---------- Task / Notification ----------

    public static TaskDtos.TaskResponse task(Task t) {
        return new TaskDtos.TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getType(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getFullName() : null,
                t.getLead() != null ? t.getLead().getId() : null,
                t.getLead() != null ? t.getLead().getReference() : null,
                t.getDueAt(), t.getReminderAt(), t.getPriority(), t.getStatus(),
                t.getCompletedAt(), t.getCreatedAt());
    }

    public static NotificationDtos.NotificationResponse notification(Notification n) {
        return new NotificationDtos.NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getEntityType(), n.getEntityId(), n.isRead(), n.getReadAt(), n.getCreatedAt());
    }
}

package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class LeadDtos {
    private LeadDtos() {}

    public record CreateLeadRequest(
            @NotBlank String customerName,
            @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Invalid mobile number") String mobile,
            @Email String email,
            String source,
            SourceChannel sourceChannel,
            PropertyType propertyType,
            @PositiveOrZero BigDecimal budgetMin,
            @PositiveOrZero BigDecimal budgetMax,
            String preferredLocation,
            LeadPriority priority,
            Long assignedUserId,
            Long interestedPropertyId,
            Instant nextFollowUpAt,
            String notes) {}

    public record UpdateLeadRequest(
            @NotBlank String customerName,
            @NotBlank String mobile,
            @Email String email,
            String source,
            SourceChannel sourceChannel,
            PropertyType propertyType,
            @PositiveOrZero BigDecimal budgetMin,
            @PositiveOrZero BigDecimal budgetMax,
            String preferredLocation,
            LeadPriority priority,
            Long interestedPropertyId,
            Instant nextFollowUpAt,
            String notes) {}

    public record LeadResponse(
            Long id, String reference, String customerName, String mobile, String email,
            String source, SourceChannel sourceChannel, PropertyType propertyType,
            BigDecimal budgetMin, BigDecimal budgetMax, String preferredLocation,
            LeadStatus status, LeadPriority priority,
            Long assignedUserId, String assignedUserName,
            Long interestedPropertyId, String interestedPropertyTitle,
            Long convertedCustomerId,
            Instant nextFollowUpAt, Instant lastContactedAt,
            String notes, String lostReason,
            Instant createdAt, Instant updatedAt, String createdBy) {}

    public record LeadListItem(
            Long id, String reference, String customerName, String mobile,
            SourceChannel sourceChannel, PropertyType propertyType, LeadStatus status,
            LeadPriority priority, String assignedUserName, Instant nextFollowUpAt, Instant createdAt) {}

    public record TransitionRequest(
            @NotBlank String targetStatus,
            String note,
            String lostReason,
            Instant followUpAt,
            Long propertyId,
            Instant siteVisitAt,
            Long salesExecutiveId) {}

    public record AddNoteRequest(@NotBlank String note) {}

    public record ActivityResponse(
            Long id, ActivityType type, LeadStatus fromStatus, LeadStatus toStatus,
            String summary, String detail, String actorName, Instant occurredAt) {}

    public record ImportResultResponse(int totalRows, int imported, int skipped, List<String> errors) {}
}

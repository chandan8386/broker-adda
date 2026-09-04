package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.SiteVisitResult;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class SiteVisitDtos {
    private SiteVisitDtos() {}

    public record ScheduleRequest(
            @NotNull Long leadId,
            Long propertyId,
            Long salesExecutiveId,
            @NotNull Instant scheduledAt,
            Boolean pickupRequired,
            String feedback) {}

    public record RescheduleRequest(@NotNull Instant scheduledAt, String reason) {}

    public record ResultRequest(
            @NotNull SiteVisitResult result,
            String feedback,
            Integer rating,
            Instant nextFollowUpAt) {}

    public record SiteVisitResponse(
            Long id, Long leadId, String leadName, String leadMobile,
            Long propertyId, String propertyTitle,
            Long salesExecutiveId, String salesExecutiveName,
            Instant scheduledAt, SiteVisitStatus status, SiteVisitResult result,
            String feedback, boolean pickupRequired, Integer rating,
            Instant completedAt, Instant createdAt) {}
}

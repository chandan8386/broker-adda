package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.CallDirection;
import com.trishakti.crm.domain.enums.CallDisposition;
import com.trishakti.crm.domain.enums.CallOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

public final class CallDtos {
    private CallDtos() {}

    public record LogCallRequest(
            @NotNull Long leadId,
            CallDirection direction,
            @NotNull CallOutcome outcome,
            CallDisposition disposition,
            @PositiveOrZero Integer durationSeconds,
            String notes,
            Instant nextFollowUpAt) {}

    public record CallResponse(
            Long id, Long leadId, String leadName, Long callerId, String callerName,
            CallDirection direction, CallOutcome outcome, CallDisposition disposition,
            Integer durationSeconds, String notes, Instant calledAt, Instant nextFollowUpAt) {}

    public record FollowUpRequest(
            @NotNull Long leadId,
            @NotNull Instant dueAt,
            String channel,
            String notes) {}

    public record FollowUpResponse(
            Long id, Long leadId, String leadName, Long ownerId, String ownerName,
            Instant dueAt, String channel, String status, String notes, Instant completedAt) {}
}

package com.trishakti.crm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class AssignmentDtos {
    private AssignmentDtos() {}

    public record AssignRequest(
            @NotEmpty List<Long> leadIds,
            @NotNull Long toUserId,
            String reason) {}

    public record AutoAssignRequest(
            @NotEmpty List<Long> leadIds,
            @NotEmpty List<Long> userIds,
            String strategy) {}

    public record AssignmentHistoryResponse(
            Long id, Long leadId, Long fromUserId, String fromUserName,
            Long toUserId, String toUserName, String assignedByName,
            String reason, Instant assignedAt) {}
}

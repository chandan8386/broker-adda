package com.trishakti.crm.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record DashboardSummary(
            long totalLeads,
            long newLeads,
            long todaysFollowUps,
            long overdueFollowUps,
            long interestedLeads,
            long siteVisitsScheduled,
            long siteVisitsCompleted,
            long bookings,
            long purchases,
            long lostLeads,
            double leadConversionPercent,
            Map<String, Long> leadsByStatus,
            BigDecimal totalSalesValue) {}

    public record SourcePerformanceRow(String source, long totalLeads, long converted, double conversionPercent) {}

    public record CallingPerformanceRow(
            Long userId, String name, long totalCalls, long connected,
            long interested, long siteVisits, double connectRate) {}

    public record SalesPerformanceRow(
            Long userId, String name, long assignedLeads, long interested,
            long purchased, double closeRate) {}

    public record ReportsResponse(
            List<SourcePerformanceRow> sourceWise,
            List<CallingPerformanceRow> callingTeam,
            List<SalesPerformanceRow> salesTeam) {}
}

package com.trishakti.crm.service;

import com.trishakti.crm.domain.enums.BookingStatus;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PurchaseStatus;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import com.trishakti.crm.dto.DashboardDtos.CallingPerformanceRow;
import com.trishakti.crm.dto.DashboardDtos.DashboardSummary;
import com.trishakti.crm.dto.DashboardDtos.ReportsResponse;
import com.trishakti.crm.dto.DashboardDtos.SalesPerformanceRow;
import com.trishakti.crm.dto.DashboardDtos.SourcePerformanceRow;
import com.trishakti.crm.repository.BookingRepository;
import com.trishakti.crm.repository.CallLogRepository;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.PurchaseRepository;
import com.trishakti.crm.repository.SiteVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<LeadStatus> INTERESTED_STATES = List.of(
            LeadStatus.INTERESTED, LeadStatus.SITE_VISIT_SCHEDULED, LeadStatus.SITE_VISIT_DONE,
            LeadStatus.NEGOTIATION, LeadStatus.BOOKING, LeadStatus.PURCHASED);
    private static final List<LeadStatus> CLOSED_STATES = List.of(
            LeadStatus.PURCHASED, LeadStatus.CLOSED, LeadStatus.LOST);

    private final LeadRepository leadRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final BookingRepository bookingRepository;
    private final PurchaseRepository purchaseRepository;
    private final CallLogRepository callLogRepository;

    public DashboardService(LeadRepository leadRepository, SiteVisitRepository siteVisitRepository,
                            BookingRepository bookingRepository, PurchaseRepository purchaseRepository,
                            CallLogRepository callLogRepository) {
        this.leadRepository = leadRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.bookingRepository = bookingRepository;
        this.purchaseRepository = purchaseRepository;
        this.callLogRepository = callLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary() {
        Instant startOfDay = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        Instant endOfDay = startOfDay.plusSeconds(86_400);

        long totalLeads = leadRepository.countByDeletedFalse();
        long newLeads = leadRepository.countByStatusAndDeletedFalse(LeadStatus.NEW);
        long todaysFollowUps = leadRepository.countFollowUpsBetween(startOfDay, endOfDay);
        long overdueFollowUps = leadRepository.countOverdueFollowUps(startOfDay, CLOSED_STATES);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (LeadStatus s : LeadStatus.values()) byStatus.put(s.name(), 0L);
        leadRepository.countGroupedByStatus().forEach(r -> byStatus.put(r.getStatus().name(), r.getCount()));

        long interested = INTERESTED_STATES.stream().mapToLong(s -> byStatus.getOrDefault(s.name(), 0L)).sum();
        long siteVisitsScheduled = siteVisitRepository.countByStatus(SiteVisitStatus.SCHEDULED)
                + siteVisitRepository.countByStatus(SiteVisitStatus.RESCHEDULED);
        long siteVisitsCompleted = siteVisitRepository.countByStatus(SiteVisitStatus.COMPLETED);
        long bookings = bookingRepository.count() - bookingRepository.countByStatus(BookingStatus.CANCELLED);
        long purchases = purchaseRepository.count();
        long lost = byStatus.getOrDefault(LeadStatus.LOST.name(), 0L);

        double conversion = totalLeads == 0 ? 0.0
                : Math.round((purchases * 10000.0) / totalLeads) / 100.0;

        return new DashboardSummary(
                totalLeads, newLeads, todaysFollowUps, overdueFollowUps, interested,
                siteVisitsScheduled, siteVisitsCompleted, bookings, purchases, lost,
                conversion, byStatus, purchaseRepository.totalSalesValue());
    }

    @Transactional(readOnly = true)
    public ReportsResponse reports(Instant from, Instant to) {
        Instant start = from != null ? from : Instant.now().minusSeconds(30L * 86_400);
        Instant end = to != null ? to : Instant.now();

        List<SourcePerformanceRow> sourceWise = leadRepository.sourcePerformance().stream()
                .map(r -> new SourcePerformanceRow(
                        r.getSource() != null ? r.getSource().name() : "OTHER",
                        r.getTotal(), r.getWon(),
                        r.getTotal() == 0 ? 0.0 : round(r.getWon() * 100.0 / r.getTotal())))
                .sorted((a, b) -> Long.compare(b.totalLeads(), a.totalLeads()))
                .toList();

        List<CallingPerformanceRow> callingTeam = callLogRepository.callerPerformance(start, end).stream()
                .map(r -> new CallingPerformanceRow(
                        r.getUserId(), r.getName(), r.getTotalCalls(), r.getConnected(),
                        r.getInterested(), r.getSiteVisits(),
                        r.getTotalCalls() == 0 ? 0.0 : round(r.getConnected() * 100.0 / r.getTotalCalls())))
                .sorted((a, b) -> Long.compare(b.totalCalls(), a.totalCalls()))
                .toList();

        List<SalesPerformanceRow> salesTeam = leadRepository.userPerformance(INTERESTED_STATES).stream()
                .map(r -> new SalesPerformanceRow(
                        r.getUserId(), r.getName(), r.getTotal(), r.getInterested(), r.getPurchased(),
                        r.getTotal() == 0 ? 0.0 : round(r.getPurchased() * 100.0 / r.getTotal())))
                .sorted((a, b) -> Long.compare(b.purchased(), a.purchased()))
                .toList();

        return new ReportsResponse(sourceWise, callingTeam, salesTeam);
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

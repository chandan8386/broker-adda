package com.trishakti.crm.web;

import com.trishakti.crm.dto.DashboardDtos.DashboardSummary;
import com.trishakti.crm.dto.DashboardDtos.ReportsResponse;
import com.trishakti.crm.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard & Reports")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Headline KPIs: totals, today's follow-ups, interested, site visits, bookings, purchases, conversion %")
    public DashboardSummary summary() {
        return dashboardService.summary();
    }

    @GetMapping("/reports")
    @Operation(summary = "Source-wise, calling-team and sales-team performance over a date range")
    public ReportsResponse reports(@RequestParam(required = false) Instant from,
                                   @RequestParam(required = false) Instant to) {
        return dashboardService.reports(from, to);
    }
}

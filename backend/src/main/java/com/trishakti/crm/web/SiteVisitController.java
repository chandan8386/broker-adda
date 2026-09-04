package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import com.trishakti.crm.dto.SiteVisitDtos.RescheduleRequest;
import com.trishakti.crm.dto.SiteVisitDtos.ResultRequest;
import com.trishakti.crm.dto.SiteVisitDtos.ScheduleRequest;
import com.trishakti.crm.dto.SiteVisitDtos.SiteVisitResponse;
import com.trishakti.crm.service.SiteVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/site-visits")
@Tag(name = "Site Visit Management")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    public SiteVisitController(SiteVisitService siteVisitService) {
        this.siteVisitService = siteVisitService;
    }

    @PostMapping
    @Operation(summary = "Schedule a site visit for a lead")
    public SiteVisitResponse schedule(@Valid @RequestBody ScheduleRequest request) {
        return siteVisitService.schedule(request);
    }

    @GetMapping("/{id}")
    public SiteVisitResponse get(@PathVariable Long id) {
        return siteVisitService.get(id);
    }

    @GetMapping
    @Operation(summary = "List site visits by status")
    public PageResponse<SiteVisitResponse> byStatus(
            @RequestParam(defaultValue = "SCHEDULED") SiteVisitStatus status,
            @PageableDefault(size = 20, sort = "scheduledAt") Pageable pageable) {
        return siteVisitService.byStatus(status, pageable);
    }

    @GetMapping("/lead/{leadId}")
    public List<SiteVisitResponse> forLead(@PathVariable Long leadId) {
        return siteVisitService.forLead(leadId);
    }

    @PostMapping("/{id}/reschedule")
    public SiteVisitResponse reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request) {
        return siteVisitService.reschedule(id, request);
    }

    @PostMapping("/{id}/result")
    @Operation(summary = "Record the outcome of a completed site visit")
    public SiteVisitResponse recordResult(@PathVariable Long id, @Valid @RequestBody ResultRequest request) {
        return siteVisitService.recordResult(id, request);
    }

    @PostMapping("/{id}/cancel")
    public SiteVisitResponse cancel(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return siteVisitService.cancel(id, body != null ? body.get("reason") : null);
    }
}

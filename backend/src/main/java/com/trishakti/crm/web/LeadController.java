package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SourceChannel;
import com.trishakti.crm.dto.AssignmentDtos.AssignmentHistoryResponse;
import com.trishakti.crm.dto.LeadDtos.ActivityResponse;
import com.trishakti.crm.dto.LeadDtos.AddNoteRequest;
import com.trishakti.crm.dto.LeadDtos.CreateLeadRequest;
import com.trishakti.crm.dto.LeadDtos.ImportResultResponse;
import com.trishakti.crm.dto.LeadDtos.LeadListItem;
import com.trishakti.crm.dto.LeadDtos.LeadResponse;
import com.trishakti.crm.dto.LeadDtos.TransitionRequest;
import com.trishakti.crm.dto.LeadDtos.UpdateLeadRequest;
import com.trishakti.crm.security.SecurityUtils;
import com.trishakti.crm.service.LeadAssignmentService;
import com.trishakti.crm.service.LeadImportExportService;
import com.trishakti.crm.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/leads")
@Tag(name = "Lead Management")
public class LeadController {

    private final LeadService leadService;
    private final LeadAssignmentService assignmentService;
    private final LeadImportExportService importExportService;

    public LeadController(LeadService leadService, LeadAssignmentService assignmentService,
                          LeadImportExportService importExportService) {
        this.leadService = leadService;
        this.assignmentService = assignmentService;
        this.importExportService = importExportService;
    }

    @GetMapping
    @Operation(summary = "Search / filter / sort / paginate leads (role-scoped)")
    public PageResponse<LeadListItem> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) SourceChannel sourceChannel,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) Long assignedUserId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) BigDecimal budgetMin,
            @RequestParam(required = false) BigDecimal budgetMax,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) Instant followUpFrom,
            @RequestParam(required = false) Instant followUpTo,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return leadService.search(q, status, sourceChannel, propertyType, priority, assignedUserId, unassigned,
                budgetMin, budgetMax, createdFrom, createdTo, followUpFrom, followUpTo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single lead by id")
    public LeadResponse get(@PathVariable Long id) {
        return leadService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a lead")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public LeadResponse create(@Valid @RequestBody CreateLeadRequest request) {
        return leadService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lead details")
    public LeadResponse update(@PathVariable Long id, @Valid @RequestBody UpdateLeadRequest request) {
        return leadService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a lead")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leadService.softDelete(id);
    }

    @PostMapping("/{id}/transition")
    @Operation(summary = "Move a lead through the workflow (NEW → ASSIGNED → CALLING → ... → PURCHASED/LOST)")
    public LeadResponse transition(@PathVariable Long id, @Valid @RequestBody TransitionRequest request) {
        return leadService.transition(id, request);
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Activity / history timeline for a lead")
    public List<ActivityResponse> activities(@PathVariable Long id) {
        return leadService.activities(id);
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Append a note to a lead's timeline")
    public ActivityResponse addNote(@PathVariable Long id, @Valid @RequestBody AddNoteRequest request) {
        return leadService.addNote(id, request.note());
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "Assignment history for a lead")
    public List<AssignmentHistoryResponse> assignmentHistory(@PathVariable Long id) {
        return assignmentService.history(id);
    }

    // ---------- Import / Export ----------

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import leads from a CSV file")
    public ImportResultResponse importLeads(@RequestPart("file") MultipartFile file) {
        return importExportService.importCsv(file);
    }

    @GetMapping("/import/template")
    @Operation(summary = "Download the CSV import template")
    public ResponseEntity<byte[]> importTemplate() {
        return csv("lead-import-template.csv", importExportService.template());
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export leads as CSV, honouring the same filters and role scoping as the list endpoint")
    public void export(
            HttpServletResponse response,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) SourceChannel sourceChannel,
            @RequestParam(required = false) PropertyType propertyType,
            @RequestParam(required = false) LeadPriority priority,
            @RequestParam(required = false) Long assignedUserId) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads-export.csv\"");

        // Written synchronously on the request thread. StreamingResponseBody would dispatch this
        // asynchronously, where the SecurityContext is gone and the security filter chain rejects
        // the re-dispatch after the response has already been committed.
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            importExportService.streamCsv(writer, SecurityUtils.currentScopeUserId(), q, status,
                    sourceChannel, propertyType, priority, assignedUserId);
        }
    }

    private ResponseEntity<byte[]> csv(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }
}

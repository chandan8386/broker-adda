package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.AuditLog;
import com.trishakti.crm.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@Tag(name = "Audit Logs")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    @Operation(summary = "Search audit log entries (ADMIN only)")
    public PageResponse<AuditLog> search(@RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String entityType,
                                         @RequestParam(required = false) String action,
                                         @PageableDefault(size = 30, sort = "at") Pageable pageable) {
        return auditService.search(actor, entityType, action, pageable);
    }
}

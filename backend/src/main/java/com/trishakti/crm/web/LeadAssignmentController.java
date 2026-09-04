package com.trishakti.crm.web;

import com.trishakti.crm.dto.AssignmentDtos.AssignRequest;
import com.trishakti.crm.dto.AssignmentDtos.AutoAssignRequest;
import com.trishakti.crm.service.LeadAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/lead-assignments")
@Tag(name = "Lead Assignment")
@PreAuthorize("hasAnyRole('ADMIN','SALES_MANAGER')")
public class LeadAssignmentController {

    private final LeadAssignmentService assignmentService;

    public LeadAssignmentController(LeadAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign one or more leads to a user")
    public Map<String, Integer> assign(@Valid @RequestBody AssignRequest request) {
        return Map.of("assigned", assignmentService.assign(request));
    }

    @PostMapping("/auto-assign")
    @Operation(summary = "Round-robin distribute leads across a set of users")
    public Map<String, Integer> autoAssign(@Valid @RequestBody AutoAssignRequest request) {
        return Map.of("assigned", assignmentService.autoAssign(request));
    }
}

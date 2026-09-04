package com.trishakti.crm.web;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.dto.CallDtos.CallResponse;
import com.trishakti.crm.dto.CallDtos.FollowUpRequest;
import com.trishakti.crm.dto.CallDtos.FollowUpResponse;
import com.trishakti.crm.dto.CallDtos.LogCallRequest;
import com.trishakti.crm.service.CallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calls")
@Tag(name = "Calling & Follow-up")
public class CallController {

    private final CallService callService;

    public CallController(CallService callService) {
        this.callService = callService;
    }

    @PostMapping
    @Operation(summary = "Log a call against a lead (updates status + timeline, optional next follow-up)")
    public CallResponse logCall(@Valid @RequestBody LogCallRequest request) {
        return callService.logCall(request);
    }

    @GetMapping("/lead/{leadId}")
    @Operation(summary = "Call history for a lead")
    public PageResponse<CallResponse> callsForLead(@PathVariable Long leadId,
                                                   @PageableDefault(size = 20, sort = "calledAt") Pageable pageable) {
        return callService.callsForLead(leadId, pageable);
    }

    @GetMapping("/mine")
    @Operation(summary = "Calls made by the current user")
    public PageResponse<CallResponse> myCalls(@PageableDefault(size = 20, sort = "calledAt") Pageable pageable) {
        return callService.myCalls(pageable);
    }

    @PostMapping("/follow-ups")
    @Operation(summary = "Schedule the next follow-up for a lead")
    public FollowUpResponse scheduleFollowUp(@Valid @RequestBody FollowUpRequest request) {
        return callService.scheduleFollowUp(request);
    }

    @PostMapping("/follow-ups/{id}/complete")
    @Operation(summary = "Mark a follow-up as done")
    public FollowUpResponse completeFollowUp(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body) {
        return callService.completeFollowUp(id, body != null ? body.get("note") : null);
    }

    @GetMapping("/follow-ups/lead/{leadId}")
    @Operation(summary = "Follow-ups for a lead")
    public List<FollowUpResponse> followUpsForLead(@PathVariable Long leadId) {
        return callService.followUpsForLead(leadId);
    }

    @GetMapping("/follow-ups/mine")
    @Operation(summary = "Pending follow-ups owned by the current user")
    public PageResponse<FollowUpResponse> myFollowUps(@PageableDefault(size = 20, sort = "dueAt") Pageable pageable) {
        return callService.myPendingFollowUps(pageable);
    }
}

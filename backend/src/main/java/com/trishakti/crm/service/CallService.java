package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.CallLog;
import com.trishakti.crm.domain.FollowUp;
import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.CallDirection;
import com.trishakti.crm.domain.enums.CallDisposition;
import com.trishakti.crm.domain.enums.CallOutcome;
import com.trishakti.crm.domain.enums.FollowUpChannel;
import com.trishakti.crm.domain.enums.FollowUpStatus;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.dto.CallDtos.CallResponse;
import com.trishakti.crm.dto.CallDtos.FollowUpRequest;
import com.trishakti.crm.dto.CallDtos.FollowUpResponse;
import com.trishakti.crm.dto.CallDtos.LogCallRequest;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.CallLogRepository;
import com.trishakti.crm.repository.FollowUpRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CallService {

    private final CallLogRepository callLogRepository;
    private final FollowUpRepository followUpRepository;
    private final UserRepository userRepository;
    private final LeadService leadService;

    public CallService(CallLogRepository callLogRepository, FollowUpRepository followUpRepository,
                       UserRepository userRepository, LeadService leadService) {
        this.callLogRepository = callLogRepository;
        this.followUpRepository = followUpRepository;
        this.userRepository = userRepository;
        this.leadService = leadService;
    }

    @Transactional
    public CallResponse logCall(LogCallRequest req) {
        Lead lead = leadService.loadVisible(req.leadId());
        User caller = currentUser();

        CallLog call = new CallLog();
        call.setLead(lead);
        call.setCaller(caller);
        call.setDirection(req.direction() != null ? req.direction() : CallDirection.OUTBOUND);
        call.setOutcome(req.outcome());
        call.setDisposition(req.disposition());
        call.setDurationSeconds(req.durationSeconds());
        call.setNotes(req.notes());
        call.setCalledAt(Instant.now());
        call.setNextFollowUpAt(req.nextFollowUpAt());
        callLogRepository.save(call);

        lead.setLastContactedAt(Instant.now());
        if (req.nextFollowUpAt() != null) {
            lead.setNextFollowUpAt(req.nextFollowUpAt());
            createFollowUp(lead, caller, req.nextFollowUpAt(), FollowUpChannel.CALL, "Auto from call log");
        }

        applyStatusFromCall(lead, req.outcome(), req.disposition());

        leadService.logActivity(lead, ActivityType.CALL, lead.getStatus(), lead.getStatus(),
                "Call: " + req.outcome() + (req.disposition() != null ? " / " + req.disposition() : ""),
                req.notes(), caller);

        return CrmMappers.call(call);
    }

    @Transactional(readOnly = true)
    public PageResponse<CallResponse> callsForLead(Long leadId, Pageable pageable) {
        leadService.loadVisible(leadId);
        return PageResponse.of(callLogRepository.findByLeadId(leadId, pageable), CrmMappers::call);
    }

    @Transactional(readOnly = true)
    public PageResponse<CallResponse> myCalls(Pageable pageable) {
        return PageResponse.of(callLogRepository.findByCallerId(SecurityUtils.currentUserId(), pageable),
                CrmMappers::call);
    }

    @Transactional
    public FollowUpResponse scheduleFollowUp(FollowUpRequest req) {
        Lead lead = leadService.loadVisible(req.leadId());
        User owner = currentUser();
        FollowUpChannel channel = req.channel() != null
                ? FollowUpChannel.valueOf(req.channel().toUpperCase()) : FollowUpChannel.CALL;
        FollowUp fu = createFollowUp(lead, owner, req.dueAt(), channel, req.notes());
        lead.setNextFollowUpAt(req.dueAt());
        leadService.logActivity(lead, ActivityType.FOLLOW_UP_SET, lead.getStatus(), lead.getStatus(),
                "Follow-up set for " + req.dueAt(), req.notes(), owner);
        return CrmMappers.followUp(fu);
    }

    @Transactional
    public FollowUpResponse completeFollowUp(Long followUpId, String note) {
        FollowUp fu = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new ResourceNotFoundException("FollowUp", followUpId));
        fu.setStatus(FollowUpStatus.DONE);
        fu.setCompletedAt(Instant.now());
        if (note != null) fu.setNotes(note);
        leadService.logActivity(fu.getLead(), ActivityType.FOLLOW_UP_DONE, fu.getLead().getStatus(),
                fu.getLead().getStatus(), "Follow-up completed", note, currentUser());
        return CrmMappers.followUp(fu);
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> followUpsForLead(Long leadId) {
        leadService.loadVisible(leadId);
        return followUpRepository.findByLeadIdOrderByDueAtDesc(leadId).stream()
                .map(CrmMappers::followUp).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUpResponse> myPendingFollowUps(Pageable pageable) {
        return PageResponse.of(
                followUpRepository.findByOwnerIdAndStatus(SecurityUtils.currentUserId(),
                        FollowUpStatus.PENDING, pageable),
                CrmMappers::followUp);
    }

    private FollowUp createFollowUp(Lead lead, User owner, Instant dueAt, FollowUpChannel channel, String notes) {
        FollowUp fu = new FollowUp();
        fu.setLead(lead);
        fu.setOwner(owner);
        fu.setDueAt(dueAt);
        fu.setChannel(channel);
        fu.setStatus(FollowUpStatus.PENDING);
        fu.setNotes(notes);
        return followUpRepository.save(fu);
    }

    private void applyStatusFromCall(Lead lead, CallOutcome outcome, CallDisposition disposition) {
        LeadStatus current = lead.getStatus();
        if (!current.isOpen()) return;
        LeadStatus target = null;
        if (disposition == CallDisposition.NOT_INTERESTED) target = LeadStatus.NOT_INTERESTED;
        else if (disposition == CallDisposition.INTERESTED) target = LeadStatus.INTERESTED;
        else if (outcome == CallOutcome.CONNECTED) target = LeadStatus.CONNECTED;
        else if (current == LeadStatus.ASSIGNED || current == LeadStatus.CALLING) target = LeadStatus.NOT_CONNECTED;

        if (target != null && current != target && current.canTransitionTo(target)) {
            lead.setStatus(target);
        } else if (current == LeadStatus.ASSIGNED && current.canTransitionTo(LeadStatus.CALLING)) {
            lead.setStatus(LeadStatus.CALLING);
        }
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }
}

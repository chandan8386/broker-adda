package com.trishakti.crm.service;

import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.LeadAssignment;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.dto.AssignmentDtos.AssignRequest;
import com.trishakti.crm.dto.AssignmentDtos.AssignmentHistoryResponse;
import com.trishakti.crm.dto.AssignmentDtos.AutoAssignRequest;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.LeadAssignmentRepository;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LeadAssignmentService {

    private final LeadRepository leadRepository;
    private final LeadAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final LeadService leadService;
    private final AuditService auditService;

    public LeadAssignmentService(LeadRepository leadRepository, LeadAssignmentRepository assignmentRepository,
                                 UserRepository userRepository, LeadService leadService, AuditService auditService) {
        this.leadRepository = leadRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.leadService = leadService;
        this.auditService = auditService;
    }

    @Transactional
    public int assign(AssignRequest req) {
        User toUser = userRepository.findById(req.toUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.toUserId()));
        User assignedBy = currentUser();
        int count = 0;
        for (Long leadId : req.leadIds()) {
            Lead lead = leadRepository.findById(leadId)
                    .orElseThrow(() -> new ResourceNotFoundException("Lead", leadId));
            recordHistory(lead, lead.getAssignedUser(), toUser, assignedBy, req.reason());
            leadService.doAssign(lead, toUser, assignedBy, req.reason());
            count++;
        }
        auditService.record("LEADS_ASSIGNED", "Lead", null, req.leadIds(), req.toUserId());
        return count;
    }

    /** Round-robin auto distribution. */
    @Transactional
    public int autoAssign(AutoAssignRequest req) {
        List<User> users = userRepository.findAllById(req.userIds());
        if (users.isEmpty()) throw new ResourceNotFoundException("No valid users to assign to");
        User assignedBy = currentUser();
        List<Long> leadIds = req.leadIds();
        int count = 0;
        for (int i = 0; i < leadIds.size(); i++) {
            User target = users.get(i % users.size());
            Lead lead = leadRepository.findById(leadIds.get(i))
                    .orElseThrow(() -> new ResourceNotFoundException("Lead", leadIds.get(0)));
            recordHistory(lead, lead.getAssignedUser(), target, assignedBy, "Auto-assigned (round-robin)");
            leadService.doAssign(lead, target, assignedBy, "Auto-assigned (round-robin)");
            count++;
        }
        auditService.record("LEADS_AUTO_ASSIGNED", "Lead", null, leadIds, req.userIds());
        return count;
    }

    @Transactional(readOnly = true)
    public List<AssignmentHistoryResponse> history(Long leadId) {
        return assignmentRepository.findByLeadIdOrderByAssignedAtDesc(leadId).stream()
                .map(CrmMappers::assignment).toList();
    }

    private void recordHistory(Lead lead, User from, User to, User by, String reason) {
        LeadAssignment history = new LeadAssignment();
        history.setLead(lead);
        history.setFromUser(from);
        history.setToUser(to);
        history.setAssignedBy(by);
        history.setReason(reason);
        history.setAssignedAt(Instant.now());
        assignmentRepository.save(history);
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }
}

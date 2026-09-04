package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.FollowUp;
import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.LeadActivity;
import com.trishakti.crm.domain.Property;
import com.trishakti.crm.domain.SiteVisit;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.LeadPriority;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.NotificationType;
import com.trishakti.crm.domain.enums.PropertyType;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import com.trishakti.crm.domain.enums.SourceChannel;
import com.trishakti.crm.dto.LeadDtos.ActivityResponse;
import com.trishakti.crm.dto.LeadDtos.CreateLeadRequest;
import com.trishakti.crm.dto.LeadDtos.LeadListItem;
import com.trishakti.crm.dto.LeadDtos.LeadResponse;
import com.trishakti.crm.dto.LeadDtos.TransitionRequest;
import com.trishakti.crm.dto.LeadDtos.UpdateLeadRequest;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.InvalidWorkflowTransitionException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.FollowUpRepository;
import com.trishakti.crm.repository.LeadActivityRepository;
import com.trishakti.crm.repository.LeadRepository;
import com.trishakti.crm.repository.PropertyRepository;
import com.trishakti.crm.repository.SiteVisitRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.trishakti.crm.service.LeadSpecifications.*;

@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final FollowUpRepository followUpRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public LeadService(LeadRepository leadRepository, LeadActivityRepository activityRepository,
                       FollowUpRepository followUpRepository, SiteVisitRepository siteVisitRepository,
                       UserRepository userRepository, PropertyRepository propertyRepository,
                       NotificationService notificationService, AuditService auditService) {
        this.leadRepository = leadRepository;
        this.activityRepository = activityRepository;
        this.followUpRepository = followUpRepository;
        this.siteVisitRepository = siteVisitRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    // ---------------- Queries ----------------

    @Transactional(readOnly = true)
    public PageResponse<LeadListItem> search(String q, LeadStatus status, SourceChannel channel,
                                             PropertyType propertyType, LeadPriority priority,
                                             Long assignedUserId, Boolean unassigned,
                                             BigDecimal budgetMin, BigDecimal budgetMax,
                                             Instant createdFrom, Instant createdTo,
                                             Instant followUpFrom, Instant followUpTo,
                                             Pageable pageable) {

        Specification<Lead> spec = allOf(
                notDeleted(), text(q), status(status), sourceChannel(channel),
                propertyType(propertyType), priority(priority),
                assignedTo(assignedUserId), unassigned(unassigned),
                budgetAtLeast(budgetMin), budgetAtMost(budgetMax),
                createdBetween(createdFrom, createdTo), followUpBetween(followUpFrom, followUpTo),
                ownedBy(SecurityUtils.currentScopeUserId()));

        return PageResponse.of(leadRepository.findAll(spec, pageable), CrmMappers::leadListItem);
    }

    @Transactional(readOnly = true)
    public LeadResponse get(Long id) {
        return CrmMappers.lead(loadVisible(id));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> activities(Long leadId) {
        loadVisible(leadId);
        return activityRepository.findByLeadIdOrderByOccurredAtDesc(leadId).stream()
                .map(CrmMappers::activity).toList();
    }

    // ---------------- Commands ----------------

    @Transactional
    public LeadResponse create(CreateLeadRequest req) {
        User creator = currentUser();
        Lead lead = new Lead();
        lead.setCustomerName(req.customerName().trim());
        lead.setMobile(req.mobile().trim());
        lead.setEmail(req.email());
        lead.setSource(req.source());
        lead.setSourceChannel(req.sourceChannel() != null ? req.sourceChannel() : SourceChannel.OTHER);
        lead.setPropertyType(req.propertyType());
        lead.setBudgetMin(req.budgetMin());
        lead.setBudgetMax(req.budgetMax());
        lead.setPreferredLocation(req.preferredLocation());
        lead.setPriority(req.priority() != null ? req.priority() : LeadPriority.WARM);
        lead.setNotes(req.notes());
        lead.setNextFollowUpAt(req.nextFollowUpAt());
        lead.setCreatedByUser(creator);
        lead.setStatus(LeadStatus.NEW);

        if (req.interestedPropertyId() != null) {
            lead.setInterestedProperty(property(req.interestedPropertyId()));
        }
        leadRepository.save(lead);
        logActivity(lead, ActivityType.CREATED, null, LeadStatus.NEW,
                "Lead created via " + lead.getSourceChannel(), null, creator);

        if (req.assignedUserId() != null) {
            doAssign(lead, user(req.assignedUserId()), creator, "Assigned on creation");
        }
        auditService.record("LEAD_CREATED", "Lead", lead.getId(), null, CrmMappers.lead(lead));
        return CrmMappers.lead(lead);
    }

    @Transactional
    public LeadResponse update(Long id, UpdateLeadRequest req) {
        Lead lead = loadVisible(id);
        LeadResponse before = CrmMappers.lead(lead);
        lead.setCustomerName(req.customerName().trim());
        lead.setMobile(req.mobile().trim());
        lead.setEmail(req.email());
        lead.setSource(req.source());
        if (req.sourceChannel() != null) lead.setSourceChannel(req.sourceChannel());
        lead.setPropertyType(req.propertyType());
        lead.setBudgetMin(req.budgetMin());
        lead.setBudgetMax(req.budgetMax());
        lead.setPreferredLocation(req.preferredLocation());
        if (req.priority() != null) lead.setPriority(req.priority());
        lead.setNotes(req.notes());
        if (req.nextFollowUpAt() != null) lead.setNextFollowUpAt(req.nextFollowUpAt());
        if (req.interestedPropertyId() != null) lead.setInterestedProperty(property(req.interestedPropertyId()));

        logActivity(lead, ActivityType.NOTE, lead.getStatus(), lead.getStatus(),
                "Lead details updated", null, currentUser());
        auditService.record("LEAD_UPDATED", "Lead", id, before, CrmMappers.lead(lead));
        return CrmMappers.lead(lead);
    }

    @Transactional
    public void softDelete(Long id) {
        Lead lead = loadVisible(id);
        lead.setDeleted(true);
        auditService.record("LEAD_DELETED", "Lead", id, null, null);
    }

    @Transactional
    public ActivityResponse addNote(Long id, String note) {
        Lead lead = loadVisible(id);
        LeadActivity activity = logActivity(lead, ActivityType.NOTE, lead.getStatus(), lead.getStatus(),
                "Note added", note, currentUser());
        return CrmMappers.activity(activity);
    }

    /** Core workflow engine. */
    @Transactional
    public LeadResponse transition(Long id, TransitionRequest req) {
        Lead lead = loadVisible(id);
        LeadStatus from = lead.getStatus();
        LeadStatus to;
        try {
            to = LeadStatus.valueOf(req.targetStatus().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Unknown lead status: " + req.targetStatus());
        }
        if (from == to) {
            throw new BusinessRuleException("Lead is already in status " + to);
        }
        if (!from.canTransitionTo(to)) {
            throw new InvalidWorkflowTransitionException(from.name(), to.name());
        }

        User actor = currentUser();
        lead.setStatus(to);
        lead.setLastContactedAt(Instant.now());

        switch (to) {
            case LOST -> lead.setLostReason(req.lostReason() != null ? req.lostReason() : "Not specified");
            case SITE_VISIT_SCHEDULED -> createSiteVisit(lead, req, actor);
            case INTERESTED -> {
                if (req.followUpAt() != null) scheduleFollowUp(lead, req.followUpAt(), actor);
            }
            case NOT_CONNECTED, CONNECTED, CALLING, NEGOTIATION -> {
                if (req.followUpAt() != null) scheduleFollowUp(lead, req.followUpAt(), actor);
            }
            default -> { /* no side effect */ }
        }

        String summary = "Status changed: " + from + " -> " + to;
        logActivity(lead, ActivityType.STATUS_CHANGE, from, to, summary, req.note(), actor);

        if (lead.getAssignedUser() != null && !lead.getAssignedUser().getId().equals(actor.getId())) {
            notificationService.push(lead.getAssignedUser(), NotificationType.LEAD_STATUS_CHANGE,
                    "Lead " + lead.getReference() + " is now " + to,
                    summary, "Lead", lead.getId());
        }
        auditService.record("LEAD_TRANSITION", "Lead", id, from.name(), to.name());
        return CrmMappers.lead(lead);
    }

    // ---------------- Assignment (used by LeadAssignmentService) ----------------

    @Transactional
    public void doAssign(Lead lead, User toUser, User assignedBy, String reason) {
        User previous = lead.getAssignedUser();
        lead.setAssignedUser(toUser);
        if (lead.getStatus() == LeadStatus.NEW) {
            lead.setStatus(LeadStatus.ASSIGNED);
        }
        ActivityType type = previous == null ? ActivityType.ASSIGNED : ActivityType.REASSIGNED;
        logActivity(lead, type, lead.getStatus(), lead.getStatus(),
                (previous == null ? "Assigned to " : "Reassigned to ") + toUser.getFullName(), reason, assignedBy);

        notificationService.push(toUser, NotificationType.LEAD_ASSIGNED,
                "New lead assigned: " + lead.getReference(),
                lead.getCustomerName() + " (" + lead.getMobile() + ")", "Lead", lead.getId());
    }

    // ---------------- Helpers ----------------

    public Lead loadVisible(Long id) {
        Lead lead = leadRepository.findWithDetailsById(id)
                .filter(l -> !l.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Lead", id));
        if (!canSee(lead)) {
            throw new ResourceNotFoundException("Lead", id);
        }
        return lead;
    }

    LeadActivity logActivity(Lead lead, ActivityType type, LeadStatus from, LeadStatus to,
                             String summary, String detail, User actor) {
        LeadActivity activity = new LeadActivity();
        activity.setLead(lead);
        activity.setType(type);
        activity.setFromStatus(from);
        activity.setToStatus(to);
        activity.setSummary(summary);
        activity.setDetail(detail);
        activity.setActor(actor);
        activity.setOccurredAt(Instant.now());
        return activityRepository.save(activity);
    }

    private void scheduleFollowUp(Lead lead, Instant dueAt, User owner) {
        lead.setNextFollowUpAt(dueAt);
        FollowUp fu = new FollowUp();
        fu.setLead(lead);
        fu.setOwner(owner);
        fu.setDueAt(dueAt);
        followUpRepository.save(fu);
        logActivity(lead, ActivityType.FOLLOW_UP_SET, lead.getStatus(), lead.getStatus(),
                "Follow-up scheduled for " + dueAt, null, owner);
    }

    private void createSiteVisit(Lead lead, TransitionRequest req, User actor) {
        if (req.siteVisitAt() == null) {
            throw new BusinessRuleException("siteVisitAt is required to schedule a site visit");
        }
        SiteVisit visit = new SiteVisit();
        visit.setLead(lead);
        visit.setScheduledAt(req.siteVisitAt());
        visit.setStatus(SiteVisitStatus.SCHEDULED);
        if (req.propertyId() != null) visit.setProperty(property(req.propertyId()));
        else if (lead.getInterestedProperty() != null) visit.setProperty(lead.getInterestedProperty());
        if (req.salesExecutiveId() != null) visit.setSalesExecutive(user(req.salesExecutiveId()));
        siteVisitRepository.save(visit);
        lead.setNextFollowUpAt(req.siteVisitAt());

        logActivity(lead, ActivityType.SITE_VISIT_SCHEDULED, lead.getStatus(), LeadStatus.SITE_VISIT_SCHEDULED,
                "Site visit scheduled for " + req.siteVisitAt(), null, actor);
        if (visit.getSalesExecutive() != null) {
            notificationService.push(visit.getSalesExecutive(), NotificationType.SITE_VISIT_SCHEDULED,
                    "Site visit scheduled: " + lead.getReference(),
                    lead.getCustomerName() + " at " + req.siteVisitAt(), "SiteVisit", visit.getId());
        }
    }

    private boolean canSee(Lead lead) {
        if (SecurityUtils.isAdminOrManager()) return true;
        Long uid = SecurityUtils.currentUserId();
        return lead.getAssignedUser() != null && lead.getAssignedUser().getId().equals(uid)
                || (lead.getCreatedByUser() != null && lead.getCreatedByUser().getId().equals(uid));
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Property property(Long id) {
        return propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property", id));
    }
}

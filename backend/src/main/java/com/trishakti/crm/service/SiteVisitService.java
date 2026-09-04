package com.trishakti.crm.service;

import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.SiteVisit;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.ActivityType;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.NotificationType;
import com.trishakti.crm.domain.enums.SiteVisitResult;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import com.trishakti.crm.dto.SiteVisitDtos.RescheduleRequest;
import com.trishakti.crm.dto.SiteVisitDtos.ResultRequest;
import com.trishakti.crm.dto.SiteVisitDtos.ScheduleRequest;
import com.trishakti.crm.dto.SiteVisitDtos.SiteVisitResponse;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.PropertyRepository;
import com.trishakti.crm.repository.SiteVisitRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final LeadService leadService;
    private final NotificationService notificationService;

    public SiteVisitService(SiteVisitRepository siteVisitRepository, PropertyRepository propertyRepository,
                            UserRepository userRepository, LeadService leadService,
                            NotificationService notificationService) {
        this.siteVisitRepository = siteVisitRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.leadService = leadService;
        this.notificationService = notificationService;
    }

    @Transactional
    public SiteVisitResponse schedule(ScheduleRequest req) {
        Lead lead = leadService.loadVisible(req.leadId());
        User actor = currentUser();

        SiteVisit visit = new SiteVisit();
        visit.setLead(lead);
        visit.setScheduledAt(req.scheduledAt());
        visit.setStatus(SiteVisitStatus.SCHEDULED);
        visit.setPickupRequired(Boolean.TRUE.equals(req.pickupRequired()));
        visit.setFeedback(req.feedback());
        if (req.propertyId() != null) {
            visit.setProperty(propertyRepository.findById(req.propertyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Property", req.propertyId())));
        } else if (lead.getInterestedProperty() != null) {
            visit.setProperty(lead.getInterestedProperty());
        }
        User exec = req.salesExecutiveId() != null ? user(req.salesExecutiveId()) : actor;
        visit.setSalesExecutive(exec);
        siteVisitRepository.save(visit);

        moveLead(lead, LeadStatus.SITE_VISIT_SCHEDULED);
        lead.setNextFollowUpAt(req.scheduledAt());
        leadService.logActivity(lead, ActivityType.SITE_VISIT_SCHEDULED, lead.getStatus(),
                LeadStatus.SITE_VISIT_SCHEDULED, "Site visit scheduled for " + req.scheduledAt(), null, actor);

        notificationService.push(exec, NotificationType.SITE_VISIT_SCHEDULED,
                "Site visit: " + lead.getReference(),
                lead.getCustomerName() + " at " + req.scheduledAt(), "SiteVisit", visit.getId());

        return CrmMappers.siteVisit(visit);
    }

    @Transactional
    public SiteVisitResponse reschedule(Long id, RescheduleRequest req) {
        SiteVisit visit = find(id);
        if (visit.getStatus() == SiteVisitStatus.COMPLETED) {
            throw new BusinessRuleException("A completed site visit cannot be rescheduled");
        }
        visit.setScheduledAt(req.scheduledAt());
        visit.setStatus(SiteVisitStatus.RESCHEDULED);
        visit.getLead().setNextFollowUpAt(req.scheduledAt());
        leadService.logActivity(visit.getLead(), ActivityType.SITE_VISIT_SCHEDULED, visit.getLead().getStatus(),
                visit.getLead().getStatus(), "Site visit rescheduled to " + req.scheduledAt(), req.reason(), currentUser());
        return CrmMappers.siteVisit(visit);
    }

    @Transactional
    public SiteVisitResponse recordResult(Long id, ResultRequest req) {
        SiteVisit visit = find(id);
        visit.setStatus(SiteVisitStatus.COMPLETED);
        visit.setResult(req.result());
        visit.setFeedback(req.feedback());
        visit.setRating(req.rating());
        visit.setCompletedAt(Instant.now());

        Lead lead = visit.getLead();
        moveLead(lead, LeadStatus.SITE_VISIT_DONE);
        if (req.result() == SiteVisitResult.READY_TO_BOOK) {
            moveLead(lead, LeadStatus.NEGOTIATION);
        } else if (req.result() == SiteVisitResult.NOT_INTERESTED) {
            moveLead(lead, LeadStatus.NOT_INTERESTED);
        }
        if (req.nextFollowUpAt() != null) lead.setNextFollowUpAt(req.nextFollowUpAt());

        leadService.logActivity(lead, ActivityType.SITE_VISIT_DONE, LeadStatus.SITE_VISIT_SCHEDULED,
                lead.getStatus(), "Site visit completed: " + req.result(), req.feedback(), currentUser());
        return CrmMappers.siteVisit(visit);
    }

    @Transactional
    public SiteVisitResponse cancel(Long id, String reason) {
        SiteVisit visit = find(id);
        visit.setStatus(SiteVisitStatus.CANCELLED);
        visit.setFeedback(reason);
        leadService.logActivity(visit.getLead(), ActivityType.SYSTEM, visit.getLead().getStatus(),
                visit.getLead().getStatus(), "Site visit cancelled", reason, currentUser());
        return CrmMappers.siteVisit(visit);
    }

    @Transactional(readOnly = true)
    public SiteVisitResponse get(Long id) {
        return CrmMappers.siteVisit(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<SiteVisitResponse> byStatus(SiteVisitStatus status, Pageable pageable) {
        return PageResponse.of(siteVisitRepository.findByStatus(status, pageable), CrmMappers::siteVisit);
    }

    @Transactional(readOnly = true)
    public List<SiteVisitResponse> forLead(Long leadId) {
        leadService.loadVisible(leadId);
        return siteVisitRepository.findByLeadIdOrderByScheduledAtDesc(leadId).stream()
                .map(CrmMappers::siteVisit).toList();
    }

    private void moveLead(Lead lead, LeadStatus target) {
        if (lead.getStatus() != target && lead.getStatus().canTransitionTo(target)) {
            lead.setStatus(target);
        }
    }

    private SiteVisit find(Long id) {
        return siteVisitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SiteVisit", id));
    }

    private User currentUser() {
        Long uid = SecurityUtils.currentUserId();
        return userRepository.findById(uid).orElseThrow(() -> new ResourceNotFoundException("User", uid));
    }

    private User user(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}

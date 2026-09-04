package com.trishakti.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trishakti.crm.common.PageResponse;
import com.trishakti.crm.domain.AuditLog;
import com.trishakti.crm.repository.AuditLogRepository;
import com.trishakti.crm.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Long entityId, Object before, Object after) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActorUsername(SecurityUtils.currentUsername());
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setBeforeJson(toJson(before));
            entry.setAfterJson(toJson(after));
            HttpServletRequest req = currentRequest();
            if (req != null) {
                entry.setIpAddress(clientIp(req));
                entry.setUserAgent(trim(req.getHeader("User-Agent")));
            }
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write audit log for {} {}: {}", action, entityType, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLog> search(String actor, String entityType, String action, Pageable pageable) {
        return PageResponse.of(auditLogRepository.search(actor, entityType, action, pageable));
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try {
            String json = objectMapper.writeValueAsString(o);
            return json.length() > 8000 ? json.substring(0, 8000) : json;
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes sra ? sra.getRequest() : null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String trim(String s) {
        return s == null ? null : (s.length() > 250 ? s.substring(0, 250) : s);
    }
}

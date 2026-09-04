package com.trishakti.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_audit_at", columnList = "at"),
        @Index(name = "idx_audit_actor", columnList = "actor_username")
})
public class AuditLog extends BaseEntity {

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "entity_type", length = 60)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    // Plain TEXT columns rather than @Lob: on PostgreSQL @Lob String is mapped to a
    // large-object OID, which needs the LO API and breaks straight reads.
    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(nullable = false)
    private Instant at = Instant.now();
}

package com.trishakti.crm.repository;

import com.trishakti.crm.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:actor is null or lower(a.actorUsername) like lower(concat('%', :actor, '%')))
              and (:entityType is null or a.entityType = :entityType)
              and (:action is null or a.action = :action)
            """)
    Page<AuditLog> search(@Param("actor") String actor,
                          @Param("entityType") String entityType,
                          @Param("action") String action,
                          Pageable pageable);
}

package com.trishakti.crm.repository;

import com.trishakti.crm.domain.SiteVisit;
import com.trishakti.crm.domain.enums.SiteVisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long>, JpaSpecificationExecutor<SiteVisit> {

    @EntityGraph(attributePaths = {"lead", "property", "salesExecutive"})
    List<SiteVisit> findByLeadIdOrderByScheduledAtDesc(Long leadId);

    @EntityGraph(attributePaths = {"lead", "property", "salesExecutive"})
    Page<SiteVisit> findByStatus(SiteVisitStatus status, Pageable pageable);

    long countByStatus(SiteVisitStatus status);

    long countByStatusAndScheduledAtBetween(SiteVisitStatus status, Instant start, Instant end);

    @Query("""
            select s from SiteVisit s
            where s.status = com.trishakti.crm.domain.enums.SiteVisitStatus.SCHEDULED
              and s.scheduledAt between :start and :end
            """)
    List<SiteVisit> findScheduledBetween(@Param("start") Instant start, @Param("end") Instant end);
}

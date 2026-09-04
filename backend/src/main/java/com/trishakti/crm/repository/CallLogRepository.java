package com.trishakti.crm.repository;

import com.trishakti.crm.domain.CallLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    @EntityGraph(attributePaths = {"caller", "lead"})
    Page<CallLog> findByLeadId(Long leadId, Pageable pageable);

    @EntityGraph(attributePaths = {"caller", "lead"})
    Page<CallLog> findByCallerId(Long callerId, Pageable pageable);

    long countByCallerIdAndCalledAtBetween(Long callerId, Instant start, Instant end);

    @Query("""
            select c.caller.id as userId, c.caller.fullName as name,
                   count(c) as totalCalls,
                   sum(case when c.outcome = com.trishakti.crm.domain.enums.CallOutcome.CONNECTED then 1 else 0 end) as connected,
                   sum(case when c.disposition = com.trishakti.crm.domain.enums.CallDisposition.INTERESTED then 1 else 0 end) as interested,
                   sum(case when c.disposition = com.trishakti.crm.domain.enums.CallDisposition.SITE_VISIT then 1 else 0 end) as siteVisits
            from CallLog c
            where c.calledAt between :start and :end and c.caller is not null
            group by c.caller.id, c.caller.fullName
            """)
    List<CallerPerformance> callerPerformance(@Param("start") Instant start, @Param("end") Instant end);

    interface CallerPerformance {
        Long getUserId(); String getName(); long getTotalCalls();
        long getConnected(); long getInterested(); long getSiteVisits();
    }
}

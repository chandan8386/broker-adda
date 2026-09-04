package com.trishakti.crm.repository;

import com.trishakti.crm.domain.CallLog;
import com.trishakti.crm.domain.enums.RoleName;
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

    /**
     * Call stats per user holding {@code role}, for the given window.
     *
     * <p>Driven from User, not CallLog, so agents who made no calls in the window still appear
     * with zeros rather than dropping off the report. The date range must stay in the ON clause:
     * in WHERE it would filter away the null rows and silently make this an inner join.
     */
    @Query("""
            select u.id as userId, u.fullName as name,
                   count(c.id) as totalCalls,
                   coalesce(sum(case when c.outcome = com.trishakti.crm.domain.enums.CallOutcome.CONNECTED then 1 else 0 end), 0) as connected,
                   coalesce(sum(case when c.disposition = com.trishakti.crm.domain.enums.CallDisposition.INTERESTED then 1 else 0 end), 0) as interested,
                   coalesce(sum(case when c.disposition = com.trishakti.crm.domain.enums.CallDisposition.SITE_VISIT then 1 else 0 end), 0) as siteVisits
            from User u
              join u.roles r
              left join CallLog c on c.caller = u and c.calledAt between :start and :end
            where r.name = :role and u.active = true
            group by u.id, u.fullName
            """)
    List<CallerPerformance> callerPerformance(@Param("role") RoleName role,
                                              @Param("start") Instant start, @Param("end") Instant end);

    interface CallerPerformance {
        Long getUserId(); String getName(); long getTotalCalls();
        long getConnected(); long getInterested(); long getSiteVisits();
    }
}

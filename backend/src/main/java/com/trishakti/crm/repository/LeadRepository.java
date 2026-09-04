package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Lead;
import com.trishakti.crm.domain.enums.LeadStatus;
import com.trishakti.crm.domain.enums.SourceChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    @EntityGraph(attributePaths = {"assignedUser", "interestedProperty", "convertedCustomer", "createdByUser"})
    Optional<Lead> findWithDetailsById(Long id);

    /**
     * Fetches the assignee alongside each lead. Both the list screen and the CSV export read
     * assignedUser.fullName, which would otherwise be a lazy proxy per row (N+1, and a
     * LazyInitializationException once the entities leave the transaction).
     * Safe with pagination: assignedUser is @ManyToOne, so this is a join, not a collection fetch.
     */
    @Override
    @EntityGraph(attributePaths = {"assignedUser"})
    Page<Lead> findAll(Specification<Lead> spec, Pageable pageable);

    boolean existsByMobileAndDeletedFalse(String mobile);

        @Query("select l.mobile from Lead l where l.deleted = false and l.mobile in :mobiles")
        Set<String> findExistingMobiles(@Param("mobiles") Set<String> mobiles);

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(LeadStatus status);

    @Query("""
            select count(l) from Lead l
            where l.deleted = false and l.nextFollowUpAt between :start and :end
            """)
    long countFollowUpsBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            select count(l) from Lead l
            where l.deleted = false and l.nextFollowUpAt < :now and l.status not in :closed
            """)
    long countOverdueFollowUps(@Param("now") Instant now, @Param("closed") List<LeadStatus> closed);

    @Query("select l.status as status, count(l) as count from Lead l where l.deleted = false group by l.status")
    List<StatusCount> countGroupedByStatus();

    @Query("""
            select coalesce(l.sourceChannel, com.trishakti.crm.domain.enums.SourceChannel.OTHER) as source,
                   count(l) as total,
                   sum(case when l.status = com.trishakti.crm.domain.enums.LeadStatus.PURCHASED then 1 else 0 end) as won
            from Lead l where l.deleted = false
            group by l.sourceChannel
            """)
    List<SourcePerformance> sourcePerformance();

    @Query("""
            select l.assignedUser.id as userId, l.assignedUser.fullName as name,
                   count(l) as total,
                   sum(case when l.status in :interested then 1 else 0 end) as interested,
                   sum(case when l.status = com.trishakti.crm.domain.enums.LeadStatus.PURCHASED then 1 else 0 end) as purchased
            from Lead l
            where l.deleted = false and l.assignedUser is not null
            group by l.assignedUser.id, l.assignedUser.fullName
            """)
    List<UserLeadPerformance> userPerformance(@Param("interested") List<LeadStatus> interested);

    @Query("""
            select l from Lead l
            where l.deleted = false and l.status not in :closed
              and l.nextFollowUpAt is not null and l.nextFollowUpAt < :cutoff
            """)
    List<Lead> findDueFollowUps(@Param("cutoff") Instant cutoff, @Param("closed") List<LeadStatus> closed);

    interface StatusCount { LeadStatus getStatus(); long getCount(); }
    interface SourcePerformance { SourceChannel getSource(); long getTotal(); long getWon(); }
    interface UserLeadPerformance {
        Long getUserId(); String getName(); long getTotal(); long getInterested(); long getPurchased();
    }
}

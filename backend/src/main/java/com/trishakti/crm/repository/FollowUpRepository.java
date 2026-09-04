package com.trishakti.crm.repository;

import com.trishakti.crm.domain.FollowUp;
import com.trishakti.crm.domain.enums.FollowUpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    @EntityGraph(attributePaths = {"lead", "owner"})
    List<FollowUp> findByLeadIdOrderByDueAtDesc(Long leadId);

    @EntityGraph(attributePaths = {"lead", "owner"})
    Page<FollowUp> findByOwnerIdAndStatus(Long ownerId, FollowUpStatus status, Pageable pageable);

    @Query("""
            select f from FollowUp f
            where f.status = com.trishakti.crm.domain.enums.FollowUpStatus.PENDING
              and f.notified = false and f.dueAt <= :cutoff
            """)
    List<FollowUp> findDueForReminder(@Param("cutoff") Instant cutoff);

    long countByOwnerIdAndStatusAndDueAtBetween(Long ownerId, FollowUpStatus status, Instant start, Instant end);
}

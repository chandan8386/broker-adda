package com.trishakti.crm.repository;

import com.trishakti.crm.domain.LeadActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadActivityRepository extends JpaRepository<LeadActivity, Long> {

    @EntityGraph(attributePaths = "actor")
    List<LeadActivity> findByLeadIdOrderByOccurredAtDesc(Long leadId);

    @EntityGraph(attributePaths = "actor")
    Page<LeadActivity> findByLeadId(Long leadId, Pageable pageable);
}

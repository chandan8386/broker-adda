package com.trishakti.crm.repository;

import com.trishakti.crm.domain.LeadAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {

    @EntityGraph(attributePaths = {"fromUser", "toUser", "assignedBy"})
    List<LeadAssignment> findByLeadIdOrderByAssignedAtDesc(Long leadId);
}

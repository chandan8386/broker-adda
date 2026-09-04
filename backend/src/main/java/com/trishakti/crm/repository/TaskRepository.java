package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Task;
import com.trishakti.crm.domain.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignee", "lead"})
    Page<Task> findByAssigneeId(Long assigneeId, Pageable pageable);

    @EntityGraph(attributePaths = {"assignee", "lead"})
    Page<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status, Pageable pageable);

    List<Task> findByLeadIdOrderByDueAtAsc(Long leadId);

    @Query("""
            select t from Task t
            where t.status in (com.trishakti.crm.domain.enums.TaskStatus.OPEN, com.trishakti.crm.domain.enums.TaskStatus.IN_PROGRESS)
              and t.notified = false and t.reminderAt is not null and t.reminderAt <= :cutoff
            """)
    List<Task> findDueForReminder(@Param("cutoff") Instant cutoff);

    long countByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);
}

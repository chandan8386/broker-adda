package com.trishakti.crm.repository;

import com.trishakti.crm.domain.PropertyProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyProjectRepository extends JpaRepository<PropertyProject, Long> {
    Optional<PropertyProject> findByNameIgnoreCase(String name);
}

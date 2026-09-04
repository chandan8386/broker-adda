package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Role;
import com.trishakti.crm.domain.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
    boolean existsByName(RoleName name);
}

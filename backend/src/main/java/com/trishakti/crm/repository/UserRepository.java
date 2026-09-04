package com.trishakti.crm.repository;

import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("select u from User u where lower(u.username) = lower(:login) or lower(u.email) = lower(:login)")
    Optional<User> findByLogin(@Param("login") String login);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            select distinct u from User u join u.roles r
            where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%'))
                   or lower(u.username) like lower(concat('%', :q, '%')))
              and (:role is null or r.name = :role)
              and (:active is null or u.active = :active)
            """)
    Page<User> search(@Param("q") String q, @Param("role") RoleName role,
                      @Param("active") Boolean active, Pageable pageable);

    @Query("select u from User u join u.roles r where r.name = :role and u.active = true")
    List<User> findActiveByRole(@Param("role") RoleName role);
}

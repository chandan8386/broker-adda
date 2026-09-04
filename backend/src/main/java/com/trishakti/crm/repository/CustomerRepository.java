package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    @Query("""
            select c from Customer c
            where (:q is null or lower(c.fullName) like lower(concat('%', :q, '%'))
                   or c.mobile like concat('%', :q, '%')
                   or lower(c.email) like lower(concat('%', :q, '%')))
            """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);
}

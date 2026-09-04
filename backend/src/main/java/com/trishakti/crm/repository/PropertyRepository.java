package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Property;
import com.trishakti.crm.domain.enums.PropertyStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    @Query("""
            select p from Property p
            where (:q is null or lower(p.title) like lower(concat('%', :q, '%'))
                   or lower(p.location) like lower(concat('%', :q, '%'))
                   or lower(p.city) like lower(concat('%', :q, '%')))
              and (:type is null or p.propertyType = :type)
              and (:status is null or p.status = :status)
            """)
    Page<Property> search(@Param("q") String q,
                          @Param("type") PropertyType type,
                          @Param("status") PropertyStatus status,
                          Pageable pageable);

    long countByStatus(PropertyStatus status);
}

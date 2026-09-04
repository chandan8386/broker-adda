package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Purchase;
import com.trishakti.crm.domain.enums.PurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @EntityGraph(attributePaths = {"booking", "property", "customer"})
    Optional<Purchase> findWithDetailsById(Long id);

    Optional<Purchase> findByBookingId(Long bookingId);

    @EntityGraph(attributePaths = {"booking", "property", "customer"})
    Page<Purchase> findAllBy(Pageable pageable);

    long countByStatus(PurchaseStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);

    @Query("select coalesce(sum(p.finalPrice), 0) from Purchase p")
    BigDecimal totalSalesValue();
}

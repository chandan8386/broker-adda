package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Booking;
import com.trishakti.crm.domain.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"lead", "property", "customer", "salesExecutive", "siteVisit"})
    Optional<Booking> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"lead", "property", "customer", "salesExecutive"})
    Page<Booking> findByStatus(BookingStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"lead", "property", "customer", "salesExecutive"})
    Page<Booking> findBySalesExecutiveId(Long salesExecutiveId, Pageable pageable);

    boolean existsByBookingNumber(String bookingNumber);

    long countByStatus(BookingStatus status);

    long countByCreatedAtBetween(Instant start, Instant end);
}

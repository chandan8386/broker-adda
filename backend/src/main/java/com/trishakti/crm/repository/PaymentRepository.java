package com.trishakti.crm.repository;

import com.trishakti.crm.domain.Payment;
import com.trishakti.crm.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByDueDateAsc(Long bookingId);

    List<Payment> findByPurchaseIdOrderByDueDateAsc(Long purchaseId);

    @Query("select p from Payment p where p.status in :statuses and p.dueDate <= :date")
    List<Payment> findDueOrOverdue(@Param("statuses") List<PaymentStatus> statuses, @Param("date") LocalDate date);

    long countByStatus(PaymentStatus status);
}

package com.trishakti.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_mobile", columnNames = "mobile"),
        indexes = @Index(name = "idx_customer_mobile", columnList = "mobile"))
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(name = "alt_mobile", length = 20)
    private String altMobile;

    @Column(length = 150)
    private String email;

    @Column(length = 300)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(name = "id_proof_type", length = 40)
    private String idProofType;

    @Column(name = "id_proof_number", length = 60)
    private String idProofNumber;

    @Column(length = 1000)
    private String notes;

    @Column(name = "source_lead_id")
    private Long sourceLeadId;
}

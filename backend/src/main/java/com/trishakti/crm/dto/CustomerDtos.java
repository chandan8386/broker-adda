package com.trishakti.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class CustomerDtos {
    private CustomerDtos() {}

    public record CustomerRequest(
            @NotBlank String fullName,
            @NotBlank String mobile,
            String altMobile,
            @Email String email,
            String address,
            String city,
            String idProofType,
            String idProofNumber,
            String notes) {}

    public record CustomerResponse(
            Long id, String fullName, String mobile, String altMobile, String email,
            String address, String city, String idProofType, String idProofNumber,
            String notes, Long sourceLeadId, Instant createdAt) {}
}

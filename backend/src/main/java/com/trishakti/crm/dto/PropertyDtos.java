package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.PropertyStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public final class PropertyDtos {
    private PropertyDtos() {}

    public record PropertyRequest(
            Long projectId,
            @NotBlank String title,
            @NotNull PropertyType propertyType,
            String unitNumber,
            String location,
            String city,
            @PositiveOrZero BigDecimal areaSqft,
            @PositiveOrZero BigDecimal price,
            Integer bedrooms,
            Integer bathrooms,
            String facing,
            PropertyStatus status,
            String description) {}

    public record PropertyResponse(
            Long id, Long projectId, String projectName, String title, PropertyType propertyType,
            String unitNumber, String location, String city, BigDecimal areaSqft, BigDecimal price,
            Integer bedrooms, Integer bathrooms, String facing, PropertyStatus status, String description) {}

    public record ProjectRequest(@NotBlank String name, String location, String city, String description, Boolean active) {}

    public record ProjectResponse(Long id, String name, String location, String city, String description, boolean active) {}
}

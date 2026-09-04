package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.PropertyStatus;
import com.trishakti.crm.domain.enums.PropertyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "property", indexes = {
        @Index(name = "idx_property_type_status", columnList = "property_type,status"),
        @Index(name = "idx_property_city_loc", columnList = "city,location")
})
public class Property extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private PropertyProject project;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Column(name = "unit_number", length = 40)
    private String unitNumber;

    @Column(length = 150)
    private String location;

    @Column(length = 80)
    private String city;

    @Column(name = "area_sqft", precision = 12, scale = 2)
    private BigDecimal areaSqft;

    @Column(precision = 14, scale = 2)
    private BigDecimal price;

    private Integer bedrooms;
    private Integer bathrooms;

    @Column(length = 30)
    private String facing;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropertyStatus status = PropertyStatus.AVAILABLE;

    @Column(length = 1000)
    private String description;
}

package com.trishakti.crm.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "property_project")
public class PropertyProject extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 150)
    private String location;

    @Column(length = 80)
    private String city;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}

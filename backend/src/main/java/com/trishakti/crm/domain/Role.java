package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "role", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RoleName name;

    @Column(length = 200)
    private String description;
}

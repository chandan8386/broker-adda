package com.trishakti.crm.domain.enums;

public enum RoleName {
    ADMIN,
    SALES_MANAGER,
    CALLING_TEAM,
    SALES_EXECUTIVE;

    public String authority() {
        return "ROLE_" + name();
    }
}

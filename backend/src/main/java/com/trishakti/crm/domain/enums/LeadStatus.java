package com.trishakti.crm.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lead lifecycle:
 * NEW -> ASSIGNED -> CALLING -> CONNECTED / NOT_CONNECTED -> INTERESTED / NOT_INTERESTED
 *  -> SITE_VISIT_SCHEDULED -> SITE_VISIT_DONE -> NEGOTIATION -> BOOKING -> PURCHASED / CLOSED / LOST
 */
public enum LeadStatus {
    NEW,
    ASSIGNED,
    CALLING,
    CONNECTED,
    NOT_CONNECTED,
    INTERESTED,
    NOT_INTERESTED,
    SITE_VISIT_SCHEDULED,
    SITE_VISIT_DONE,
    NEGOTIATION,
    BOOKING,
    PURCHASED,
    CLOSED,
    LOST;

    private static final Map<LeadStatus, Set<LeadStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(NEW, EnumSet.of(ASSIGNED, LOST, CLOSED)),
            Map.entry(ASSIGNED, EnumSet.of(CALLING, ASSIGNED, LOST, CLOSED)),
            Map.entry(CALLING, EnumSet.of(CONNECTED, NOT_CONNECTED, LOST, CLOSED)),
            Map.entry(NOT_CONNECTED, EnumSet.of(CALLING, CONNECTED, LOST, CLOSED)),
            Map.entry(CONNECTED, EnumSet.of(INTERESTED, NOT_INTERESTED, CALLING, LOST, CLOSED)),
            Map.entry(NOT_INTERESTED, EnumSet.of(CALLING, INTERESTED, LOST, CLOSED)),
            Map.entry(INTERESTED, EnumSet.of(SITE_VISIT_SCHEDULED, NEGOTIATION, NOT_INTERESTED, LOST, CLOSED)),
            Map.entry(SITE_VISIT_SCHEDULED, EnumSet.of(SITE_VISIT_DONE, SITE_VISIT_SCHEDULED, NOT_INTERESTED, LOST, CLOSED)),
            Map.entry(SITE_VISIT_DONE, EnumSet.of(NEGOTIATION, SITE_VISIT_SCHEDULED, NOT_INTERESTED, BOOKING, LOST, CLOSED)),
            Map.entry(NEGOTIATION, EnumSet.of(BOOKING, SITE_VISIT_SCHEDULED, NOT_INTERESTED, LOST, CLOSED)),
            Map.entry(BOOKING, EnumSet.of(PURCHASED, NEGOTIATION, LOST, CLOSED)),
            Map.entry(PURCHASED, EnumSet.of(CLOSED)),
            Map.entry(LOST, EnumSet.of(CALLING, ASSIGNED)),
            Map.entry(CLOSED, EnumSet.noneOf(LeadStatus.class))
    );

    public boolean canTransitionTo(LeadStatus target) {
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public Set<LeadStatus> allowedNext() {
        return TRANSITIONS.getOrDefault(this, Set.of());
    }

    public boolean isOpen() {
        return this != PURCHASED && this != CLOSED && this != LOST;
    }

    public boolean isWon() {
        return this == PURCHASED;
    }
}

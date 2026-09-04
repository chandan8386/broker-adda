package com.trishakti.crm.domain;

import com.trishakti.crm.domain.enums.LeadStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeadStatusTransitionTest {

    @Test
    void newLeadCanOnlyMoveToAssignedLostOrClosed() {
        assertThat(LeadStatus.NEW.canTransitionTo(LeadStatus.ASSIGNED)).isTrue();
        assertThat(LeadStatus.NEW.canTransitionTo(LeadStatus.LOST)).isTrue();
        assertThat(LeadStatus.NEW.canTransitionTo(LeadStatus.CLOSED)).isTrue();
        assertThat(LeadStatus.NEW.canTransitionTo(LeadStatus.BOOKING)).isFalse();
        assertThat(LeadStatus.NEW.canTransitionTo(LeadStatus.PURCHASED)).isFalse();
    }

    @Test
    void happyPathWalksTheWholeFunnel() {
        LeadStatus[] path = {
                LeadStatus.NEW, LeadStatus.ASSIGNED, LeadStatus.CALLING, LeadStatus.CONNECTED,
                LeadStatus.INTERESTED, LeadStatus.SITE_VISIT_SCHEDULED, LeadStatus.SITE_VISIT_DONE,
                LeadStatus.NEGOTIATION, LeadStatus.BOOKING, LeadStatus.PURCHASED, LeadStatus.CLOSED
        };
        for (int i = 0; i < path.length - 1; i++) {
            assertThat(path[i].canTransitionTo(path[i + 1]))
                    .as("%s -> %s", path[i], path[i + 1])
                    .isTrue();
        }
    }

    @Test
    void closedIsTerminal() {
        assertThat(LeadStatus.CLOSED.allowedNext()).isEmpty();
    }

    @Test
    void openFlagReflectsFunnelPosition() {
        assertThat(LeadStatus.INTERESTED.isOpen()).isTrue();
        assertThat(LeadStatus.PURCHASED.isOpen()).isFalse();
        assertThat(LeadStatus.LOST.isOpen()).isFalse();
        assertThat(LeadStatus.PURCHASED.isWon()).isTrue();
    }
}

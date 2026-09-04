package com.trishakti.crm.config;

import com.trishakti.crm.domain.Role;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Reference data. The four roles must always exist regardless of whether demo
 * seeding ({@code app.seed.enabled}) is on, so login, user creation and
 * first-admin bootstrap work on a clean database.
 */
@Configuration
public class RoleInitializer {

    private static final Logger log = LoggerFactory.getLogger(RoleInitializer.class);

    @Bean
    @Order(1)
    ApplicationRunner ensureRoles(RoleRepository roleRepository) {
        return args -> {
            int created = 0;
            for (RoleName name : RoleName.values()) {
                if (!roleRepository.existsByName(name)) {
                    Role role = new Role();
                    role.setName(name);
                    role.setDescription(switch (name) {
                        case ADMIN -> "Full system access";
                        case SALES_MANAGER -> "Lead assignment, all leads, reports, team performance";
                        case CALLING_TEAM -> "Assigned leads, calls, follow-ups, schedule site visits";
                        case SALES_EXECUTIVE -> "Site visits, negotiation, bookings, purchases, payments";
                    });
                    roleRepository.save(role);
                    created++;
                }
            }
            if (created > 0) log.info("RoleInitializer created {} role(s)", created);
        };
    }
}

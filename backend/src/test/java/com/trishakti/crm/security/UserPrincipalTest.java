package com.trishakti.crm.security;

import com.trishakti.crm.domain.Role;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    void mapsUserIdentityAndRoleAuthorities() {
        User user = user(true);

        UserPrincipal principal = new UserPrincipal(user);

        assertEquals(7L, principal.getId());
        assertEquals("sales", principal.getUsername());
        assertEquals("sales@trishakti.com", principal.getEmail());
        assertEquals("Sales Executive", principal.getFullName());
        assertEquals("encoded-password", principal.getPassword());
        assertTrue(principal.isEnabled());
        assertEquals(Set.of("ROLE_SALES_EXECUTIVE"), principal.getAuthorities().stream()
                .map(Object::toString).collect(java.util.stream.Collectors.toSet()));
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
    }

    @Test
    void inactiveUserIsDisabled() {
        UserPrincipal principal = new UserPrincipal(user(false));

        assertFalse(principal.isEnabled());
    }

    private User user(boolean active) {
        User user = new User();
        user.setId(7L);
        user.setUsername("sales");
        user.setEmail("sales@trishakti.com");
        user.setFullName("Sales Executive");
        user.setPasswordHash("encoded-password");
        user.setActive(active);
        Role role = new Role();
        role.setName(RoleName.SALES_EXECUTIVE);
        user.setRoles(Set.of(role));
        return user;
    }
}

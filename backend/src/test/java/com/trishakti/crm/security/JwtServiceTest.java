package com.trishakti.crm.security;

import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-for-junit-tests-please-32bytes-minimum";
    private final JwtService jwtService = new JwtService(SECRET, 60_000, 120_000, "trishakti-test");

    @Test
    void accessTokenContainsIdentityAndAccessType() {
        UserPrincipal principal = principal();

        String token = jwtService.generateAccessToken(principal);

        assertEquals("caller", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token));
        assertFalse(jwtService.isRefreshToken(token));
        assertEquals(42L, ((Number) jwtService.claims(token).get("uid")).longValue());
        assertEquals("access", jwtService.claims(token).get("type"));
        assertEquals(Set.of("ROLE_CALLING_TEAM"), Set.copyOf((java.util.List<String>) jwtService.claims(token).get("roles")));
    }

    @Test
    void refreshTokenIsValidAndIdentifiedSeparately() {
        String token = jwtService.generateRefreshToken(principal());

        assertTrue(jwtService.isTokenValid(token));
        assertTrue(jwtService.isRefreshToken(token));
        assertEquals("caller", jwtService.extractUsername(token));
        assertEquals("refresh", jwtService.claims(token).get("type"));
    }

    @Test
    void malformedOrWronglySignedTokenIsRejected() {
        String token = jwtService.generateAccessToken(principal());
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertFalse(jwtService.isTokenValid(tampered));
        assertFalse(jwtService.isRefreshToken(tampered));
    }

    private UserPrincipal principal() {
        User user = new User();
        user.setId(42L);
        user.setUsername("caller");
        user.setEmail("caller@trishakti.com");
        user.setFullName("Test Caller");
        user.setPasswordHash("encoded");
        user.setActive(true);
        Role role = new Role();
        role.setName(RoleName.CALLING_TEAM);
        user.setRoles(Set.of(role));
        return new UserPrincipal(user);
    }
}

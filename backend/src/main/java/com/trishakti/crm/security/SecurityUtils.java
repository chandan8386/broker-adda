package com.trishakti.crm.security;

import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.exception.DomainExceptions.AccessDeniedForResourceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            return Optional.of(p);
        }
        return Optional.empty();
    }

    public static UserPrincipal requirePrincipal() {
        return currentPrincipal().orElseThrow(
                () -> new AccessDeniedForResourceException("Authentication required"));
    }

    public static Long currentUserId() {
        return requirePrincipal().getId();
    }

    public static String currentUsername() {
        return currentPrincipal().map(UserPrincipal::getUsername).orElse("system");
    }

    public static boolean hasRole(RoleName role) {
        return currentPrincipal()
                .map(p -> p.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(role.authority())))
                .orElse(false);
    }

    public static boolean isAdminOrManager() {
        return hasRole(RoleName.ADMIN) || hasRole(RoleName.SALES_MANAGER);
    }

    /**
     * Id to scope row-level queries to, or {@code null} for admins/managers who see everything.
     * Resolve this on the request thread — async/streaming responses run on a pool thread that
     * does not carry the SecurityContext.
     */
    public static Long currentScopeUserId() {
        return isAdminOrManager() ? null : currentUserId();
    }
}

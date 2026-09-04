package com.trishakti.crm.service;

import com.trishakti.crm.domain.RefreshToken;
import com.trishakti.crm.domain.Role;
import com.trishakti.crm.domain.User;
import com.trishakti.crm.domain.enums.RoleName;
import com.trishakti.crm.dto.AuthDtos.AuthResponse;
import com.trishakti.crm.dto.AuthDtos.ChangePasswordRequest;
import com.trishakti.crm.dto.AuthDtos.FirstAdminRequest;
import com.trishakti.crm.dto.AuthDtos.LoginRequest;
import com.trishakti.crm.exception.DomainExceptions.BusinessRuleException;
import com.trishakti.crm.exception.DomainExceptions.DuplicateResourceException;
import com.trishakti.crm.exception.DomainExceptions.ResourceNotFoundException;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.RefreshTokenRepository;
import com.trishakti.crm.repository.RoleRepository;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.JwtService;
import com.trishakti.crm.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, RefreshTokenRepository refreshTokenRepository,
                       JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        user.setLastLoginAt(Instant.now());

        return issueTokens(principal, user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        String hash = sha256(refreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token not recognised"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        stored.setRevoked(true);

        User user = stored.getUser();
        UserPrincipal principal = new UserPrincipal(user);
        return issueTokens(principal, user);
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::revokeAllForUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user);
    }

    @Transactional
    public AuthResponse registerFirstAdmin(FirstAdminRequest request) {
        boolean anyAdmin = !userRepository.findActiveByRole(RoleName.ADMIN).isEmpty();
        if (anyAdmin) {
            throw new BusinessRuleException("An administrator already exists. Ask an admin to create your account.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())
                || userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Username or email already in use");
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Role ADMIN not seeded"));

        User user = new User();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(Set.of(adminRole));
        user.setActive(true);
        userRepository.save(user);

        return issueTokens(new UserPrincipal(user), user);
    }

    private AuthResponse issueTokens(UserPrincipal principal, User user) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(sha256(refreshToken));
        token.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshTokenRepository.save(token);

        return new AuthResponse(accessToken, refreshToken, "Bearer",
                jwtService.getExpirationMs(), CrmMappers.userSummary(user));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

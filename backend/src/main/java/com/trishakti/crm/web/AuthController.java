package com.trishakti.crm.web;

import com.trishakti.crm.dto.AuthDtos.AuthResponse;
import com.trishakti.crm.dto.AuthDtos.ChangePasswordRequest;
import com.trishakti.crm.dto.AuthDtos.FirstAdminRequest;
import com.trishakti.crm.dto.AuthDtos.LoginRequest;
import com.trishakti.crm.dto.AuthDtos.RefreshRequest;
import com.trishakti.crm.dto.AuthDtos.UserSummary;
import com.trishakti.crm.mapper.CrmMappers;
import com.trishakti.crm.repository.UserRepository;
import com.trishakti.crm.security.SecurityUtils;
import com.trishakti.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username/email + password, returns JWT access & refresh tokens")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/register-first-admin")
    @Operation(summary = "Bootstrap the very first ADMIN account (allowed only when no admin exists)")
    public AuthResponse registerFirstAdmin(@Valid @RequestBody FirstAdminRequest request) {
        return authService.registerFirstAdmin(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens for the current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtils.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user", security = @SecurityRequirement(name = "bearerAuth"))
    public UserSummary me() {
        Long id = SecurityUtils.currentUserId();
        return CrmMappers.userSummary(userRepository.findById(id).orElseThrow());
    }
}

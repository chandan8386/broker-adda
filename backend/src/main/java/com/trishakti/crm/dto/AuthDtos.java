package com.trishakti.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMs,
            UserSummary user) {}

    public record UserSummary(
            Long id, String fullName, String username, String email,
            String phone, Set<String> roles, String team) {}

    public record FirstAdminRequest(
            @NotBlank String fullName,
            @NotBlank @Size(min = 3, max = 80) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}
}

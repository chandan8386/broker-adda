package com.trishakti.crm.dto;

import com.trishakti.crm.domain.enums.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public final class UserDtos {
    private UserDtos() {}

    public record CreateUserRequest(
            @NotBlank String fullName,
            @NotBlank @Size(min = 3, max = 80) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            String phone,
            Long teamId,
            Long managerId,
            @NotEmpty Set<RoleName> roles,
            Boolean active) {}

    public record UpdateUserRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            String phone,
            Long teamId,
            Long managerId,
            Set<RoleName> roles,
            Boolean active) {}

    public record UserResponse(
            Long id, String fullName, String username, String email, String phone,
            Long teamId, String teamName, Long managerId, String managerName,
            Set<RoleName> roles, boolean active, Instant lastLoginAt, Instant createdAt) {}

    public record TeamRequest(@NotBlank String name, String description, Long managerId, Boolean active) {}

    public record TeamResponse(Long id, String name, String description,
                               Long managerId, String managerName, boolean active, int memberCount) {}
}

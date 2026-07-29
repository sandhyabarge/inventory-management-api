package com.portfolio.inventory.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 320) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 120) String displayName) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record ChangeRoleRequest(@NotNull Role role) {}

    public record UserResponse(
            Long id,
            String email,
            String displayName,
            Role role,
            boolean active,
            Instant createdAt) {
        public static UserResponse from(UserAccount user) {
            return new UserResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getRole(),
                    user.isActive(),
                    user.getCreatedAt());
        }
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresInMs,
            UserResponse user) {}
}

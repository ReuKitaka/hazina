package com.example.hazina.users.dto;

import com.example.hazina.users.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String role,
        boolean active,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getRole().name(), u.isActive(), u.getLastLoginAt(), u.getCreatedAt()
        );
    }
}

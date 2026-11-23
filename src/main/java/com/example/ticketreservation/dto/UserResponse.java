package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;

import java.time.Instant;

public record UserResponse(
        String id,
        String username,
        String fullName,
        Role role,
        Instant createdAt
) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}

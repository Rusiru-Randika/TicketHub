package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        Role role,

        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        String password
) {
}

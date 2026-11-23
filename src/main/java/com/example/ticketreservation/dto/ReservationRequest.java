package com.example.ticketreservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReservationRequest(
        @NotBlank(message = "Event ID is required")
        String eventId,

        @Min(value = 1, message = "At least 1 seat must be reserved")
        @Max(value = 10, message = "Maximum 10 seats can be reserved at once")
        int seats
) {
}

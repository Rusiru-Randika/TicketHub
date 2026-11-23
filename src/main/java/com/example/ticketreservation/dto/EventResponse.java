package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.Event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record EventResponse(
        String id,
        String name,
        String description,
        String venue,
        LocalDateTime eventDate,
        int totalSeats,
        int availableSeats,
        BigDecimal ticketPrice,
        String organizerId,
        String organizerUsername,
        Instant createdAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getVenue(),
                event.getEventDate(),
                event.getTotalSeats(),
                event.getAvailableSeats(),
                event.getTicketPrice(),
                event.getOrganizerId(),
                event.getOrganizerUsername(),
                event.getCreatedAt()
        );
    }
}

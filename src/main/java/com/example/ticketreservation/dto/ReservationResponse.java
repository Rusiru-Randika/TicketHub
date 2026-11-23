package com.example.ticketreservation.dto;

import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(
        String id,
        String reservationCode,
        String eventId,
        String eventName,
        String customerId,
        String customerUsername,
        int seats,
        BigDecimal totalAmount,
        ReservationStatus status,
        Instant reservedAt
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getEventId(),
                reservation.getEventName(),
                reservation.getCustomerId(),
                reservation.getCustomerUsername(),
                reservation.getSeats(),
                reservation.getTotalAmount(),
                reservation.getStatus(),
                reservation.getReservedAt()
        );
    }
}

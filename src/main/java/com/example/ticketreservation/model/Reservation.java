package com.example.ticketreservation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String reservationCode;

    private String eventId;

    private String eventName;

    private String customerId;

    private String customerUsername;

    private int seats;

    private BigDecimal totalAmount;

    private ReservationStatus status;

    @Builder.Default
    private Instant reservedAt = Instant.now();
}

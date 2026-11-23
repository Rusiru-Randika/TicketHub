package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.dto.ReservationUpdateRequest;
import com.example.ticketreservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, authentication.getName()));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getMyReservations(authentication.getName()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ORGANIZER')")
    public ResponseEntity<List<ReservationResponse>> getAllReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getAllOrOrganizerReservations(authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable String id,
            @Valid @RequestBody ReservationUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reservationService.updateReservation(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ReservationResponse> cancelReservation(
            @PathVariable String id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reservationService.cancelReservation(id, authentication.getName()));
    }
}

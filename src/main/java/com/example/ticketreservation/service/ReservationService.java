package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.dto.ReservationUpdateRequest;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final AuthService authService;

    public ReservationResponse createReservation(ReservationRequest request, String username) {
        AppUser customer = authService.getByUsername(username);
        Event event = eventService.findEventOrThrow(request.eventId());

        // Atomic seat deduction in MongoDB prevents race-condition double booking
        eventService.reserveSeatsAtomically(event.getId(), request.seats());

        String code = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        BigDecimal totalAmount = event.getTicketPrice().multiply(BigDecimal.valueOf(request.seats()));

        Reservation reservation = Reservation.builder()
                .reservationCode(code)
                .eventId(event.getId())
                .eventName(event.getName())
                .customerId(customer.getId())
                .customerUsername(customer.getUsername())
                .seats(request.seats())
                .totalAmount(totalAmount)
                .status(ReservationStatus.CONFIRMED)
                .reservedAt(Instant.now())
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    public List<ReservationResponse> getMyReservations(String username) {
        return reservationRepository.findByCustomerUsernameOrderByReservedAtDesc(username)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> getAllOrOrganizerReservations(String username) {
        AppUser currentUser = authService.getByUsername(username);

        if (currentUser.getRole() == Role.ADMIN) {
            return reservationRepository.findAll()
                    .stream()
                    .map(ReservationResponse::from)
                    .toList();
        }

        // ORGANIZER: view reservations made for their own events
        List<String> myEventIds = eventRepository.findByOrganizerUsername(currentUser.getUsername())
                .stream()
                .map(Event::getId)
                .toList();

        if (myEventIds.isEmpty()) {
            return List.of();
        }

        return reservationRepository.findByEventIdInOrderByReservedAtDesc(myEventIds)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse cancelReservation(String id, String username) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found with id: " + id));

        AppUser currentUser = authService.getByUsername(username);

        if (currentUser.getRole() != Role.ADMIN && !reservation.getCustomerUsername().equals(currentUser.getUsername())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customers can only cancel their own reservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        eventService.releaseSeatsAtomically(reservation.getEventId(), reservation.getSeats());

        return ReservationResponse.from(saved);
    }

    public ReservationResponse updateReservation(String id, ReservationUpdateRequest request, String username) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Reservation not found with id: " + id));

        AppUser currentUser = authService.getByUsername(username);
        if (currentUser.getRole() != Role.ADMIN && !reservation.getCustomerUsername().equals(currentUser.getUsername())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customers can only update their own reservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cannot update a cancelled reservation");
        }

        int newSeats = request.seats();
        int oldSeats = reservation.getSeats();
        if (newSeats == oldSeats) {
            return ReservationResponse.from(reservation);
        }

        Event event = eventService.findEventOrThrow(reservation.getEventId());

        if (newSeats > oldSeats) {
            eventService.reserveSeatsAtomically(event.getId(), newSeats - oldSeats);
        } else {
            eventService.releaseSeatsAtomically(event.getId(), oldSeats - newSeats);
        }

        reservation.setSeats(newSeats);
        reservation.setTotalAmount(event.getTicketPrice().multiply(BigDecimal.valueOf(newSeats)));

        return ReservationResponse.from(reservationRepository.save(reservation));
    }
}

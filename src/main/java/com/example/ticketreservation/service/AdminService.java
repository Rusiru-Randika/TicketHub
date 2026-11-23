package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.StatsResponse;
import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;

    public StatsResponse getStats() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();

        List<Reservation> allReservations = reservationRepository.findAll();

        long confirmedCount = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .count();

        long cancelledCount = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CANCELLED)
                .count();

        long ticketsSold = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .mapToLong(Reservation::getSeats)
                .sum();

        BigDecimal totalRevenue = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                .map(Reservation::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StatsResponse(
                totalUsers,
                totalEvents,
                confirmedCount,
                cancelledCount,
                ticketsSold,
                totalRevenue
        );
    }
}

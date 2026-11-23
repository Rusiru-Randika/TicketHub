package com.example.ticketreservation.repository;

import com.example.ticketreservation.model.Reservation;
import com.example.ticketreservation.model.ReservationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {
    List<Reservation> findByCustomerUsernameOrderByReservedAtDesc(String customerUsername);
    List<Reservation> findByEventIdInOrderByReservedAtDesc(Collection<String> eventIds);
    List<Reservation> findByEventId(String eventId);
    boolean existsByEventIdAndStatus(String eventId, ReservationStatus status);
    boolean existsByCustomerUsernameAndStatus(String customerUsername, ReservationStatus status);
    List<Reservation> findByStatus(ReservationStatus status);
}

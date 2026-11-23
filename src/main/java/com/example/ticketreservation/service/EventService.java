package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ReservationRepository reservationRepository;
    private final AuthService authService;
    private final MongoTemplate mongoTemplate;

    public List<EventResponse> getAllEvents(String search) {
        List<Event> events = (search != null && !search.isBlank())
                ? eventRepository.findByNameContainingIgnoreCase(search.trim())
                : eventRepository.findAll();

        return events.stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse getEventById(String id) {
        return EventResponse.from(findEventOrThrow(id));
    }

    public Event findEventOrThrow(String id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found with id: " + id));
    }

    public EventResponse createEvent(EventRequest request, String username) {
        AppUser organizer = authService.getByUsername(username);

        Event event = Event.builder()
                .name(request.name())
                .description(request.description())
                .venue(request.venue())
                .eventDate(request.eventDate())
                .totalSeats(request.totalSeats())
                .availableSeats(request.totalSeats())
                .ticketPrice(request.ticketPrice())
                .organizerId(organizer.getId())
                .organizerUsername(organizer.getUsername())
                .createdAt(Instant.now())
                .build();

        return EventResponse.from(eventRepository.save(event));
    }

    public EventResponse updateEvent(String id, EventRequest request, String username) {
        Event event = findEventOrThrow(id);
        AppUser currentUser = authService.getByUsername(username);

        if (currentUser.getRole() != Role.ADMIN && !event.getOrganizerUsername().equals(currentUser.getUsername())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Organizers can only edit their own events");
        }

        int bookedSeats = event.getTotalSeats() - event.getAvailableSeats();
        if (request.totalSeats() < bookedSeats) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Total seats cannot be less than already booked seats (" + bookedSeats + ")"
            );
        }

        event.setName(request.name());
        event.setDescription(request.description());
        event.setVenue(request.venue());
        event.setEventDate(request.eventDate());
        event.setTotalSeats(request.totalSeats());
        event.setAvailableSeats(request.totalSeats() - bookedSeats);
        event.setTicketPrice(request.ticketPrice());

        return EventResponse.from(eventRepository.save(event));
    }

    public void deleteEvent(String id) {
        Event event = findEventOrThrow(id);

        if (reservationRepository.existsByEventIdAndStatus(event.getId(), ReservationStatus.CONFIRMED)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Cannot delete event with active confirmed reservations"
            );
        }

        eventRepository.delete(event);
    }

    /**
     * Atomically reserves seats in MongoDB to prevent double-booking under concurrency.
     */
    public void reserveSeatsAtomically(String eventId, int seats) {
        Query query = new Query(
                Criteria.where("_id").is(eventId)
                        .and("availableSeats").gte(seats)
        );
        Update update = new Update().inc("availableSeats", -seats);
        UpdateResult result = mongoTemplate.updateFirst(query, update, Event.class);

        if (result.getModifiedCount() == 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Not enough available seats for this event");
        }
    }

    /**
     * Atomically releases seats back to the event when a reservation is cancelled.
     */
    public void releaseSeatsAtomically(String eventId, int seats) {
        Query query = new Query(Criteria.where("_id").is(eventId));
        Update update = new Update().inc("availableSeats", seats);
        mongoTemplate.updateFirst(query, update, Event.class);
    }
}

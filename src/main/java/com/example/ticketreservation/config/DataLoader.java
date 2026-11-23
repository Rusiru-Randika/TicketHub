package com.example.ticketreservation.config;

import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Event;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUserIfMissing("admin", "admin123", "System Administrator", Role.ADMIN);
        AppUser organizer = seedUserIfMissing("organizer", "organizer123", "Default Event Organizer", Role.ORGANIZER);
        seedUserIfMissing("customer", "customer123", "Default Customer", Role.CUSTOMER);

        if (eventRepository.count() == 0) {
            List<Event> sampleEvents = List.of(
                    Event.builder()
                            .name("Kelaniya Tech Summit 2027")
                            .description("Annual Full-Stack & Cloud Computing Conference")
                            .venue("University of Kelaniya Main Auditorium")
                            .eventDate(LocalDateTime.of(2027, 3, 15, 9, 0))
                            .totalSeats(150)
                            .availableSeats(150)
                            .ticketPrice(new BigDecimal("1500.00"))
                            .organizerId(organizer.getId())
                            .organizerUsername(organizer.getUsername())
                            .createdAt(Instant.now())
                            .build(),
                    Event.builder()
                            .name("Colombo Symphony Night")
                            .description("Live orchestral music evening")
                            .venue("Nelum Pokuna Theatre, Colombo")
                            .eventDate(LocalDateTime.of(2027, 6, 20, 19, 0))
                            .totalSeats(100)
                            .availableSeats(100)
                            .ticketPrice(new BigDecimal("2500.00"))
                            .organizerId(organizer.getId())
                            .organizerUsername(organizer.getUsername())
                            .createdAt(Instant.now())
                            .build()
            );
            eventRepository.saveAll(sampleEvents);
            log.info("Seeded {} sample events into ticket_reservation_db", sampleEvents.size());
        }
    }

    private AppUser seedUserIfMissing(String username, String rawPassword, String fullName, Role role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    AppUser saved = userRepository.save(
                            AppUser.builder()
                                    .username(username)
                                    .password(passwordEncoder.encode(rawPassword))
                                    .fullName(fullName)
                                    .role(role)
                                    .createdAt(Instant.now())
                                    .build()
                    );
                    log.info("Seeded default user '{}' with role {}", username, role);
                    return saved;
                });
    }
}

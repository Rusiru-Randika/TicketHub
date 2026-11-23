package com.example.ticketreservation;

import com.example.ticketreservation.config.DataLoader;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
class TicketReservationApplicationTests {

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private ReservationRepository reservationRepository;

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private DataLoader dataLoader;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void contextLoadsAndJwtWorks() {
        AppUser user = AppUser.builder()
                .id("1")
                .username("PS/2022/001")
                .password(passwordEncoder.encode("PS/2022/001"))
                .fullName("Test User")
                .role(Role.CUSTOMER)
                .build();

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("PS/2022/001");
        assertThat(passwordEncoder.matches("PS/2022/001", user.getPassword())).isTrue();
    }
}

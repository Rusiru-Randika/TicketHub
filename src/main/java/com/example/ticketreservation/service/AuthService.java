package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.AuthResponse;
import com.example.ticketreservation.dto.LoginRequest;
import com.example.ticketreservation.dto.RegisterRequest;
import com.example.ticketreservation.dto.UserResponse;
import com.example.ticketreservation.dto.UserUpdateRequest;
import com.example.ticketreservation.exception.ApiException;
import com.example.ticketreservation.model.AppUser;
import com.example.ticketreservation.model.ReservationStatus;
import com.example.ticketreservation.model.Role;
import com.example.ticketreservation.repository.ReservationRepository;
import com.example.ticketreservation.repository.UserRepository;
import com.example.ticketreservation.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserResponse register(RegisterRequest request, Authentication currentAuth) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists: " + request.username());
        }

        Role requestedRole = request.role() != null ? request.role() : Role.CUSTOMER;

        if (requestedRole == Role.ADMIN || requestedRole == Role.ORGANIZER) {
            boolean isAdmin = currentAuth != null && currentAuth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Only an ADMIN can create ADMIN or ORGANIZER accounts");
            }
        }

        AppUser user = AppUser.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(requestedRole)
                .createdAt(Instant.now())
                .build();

        AppUser saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        AppUser user = getByUsername(request.username());
        String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );
    }

    public UserResponse getCurrentUser(String username) {
        return UserResponse.from(getByUsername(username));
    }

    public AppUser getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse updateUser(String id, UserUpdateRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        user.setFullName(request.fullName());
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    public void deleteUser(String id, String currentUsername) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found with id: " + id));

        if (user.getUsername().equals(currentUsername)) {
            throw new ApiException(HttpStatus.CONFLICT, "You cannot delete your own admin account");
        }

        if (reservationRepository.existsByCustomerUsernameAndStatus(user.getUsername(), ReservationStatus.CONFIRMED)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Cannot delete user with active confirmed reservations"
            );
        }

        userRepository.delete(user);
    }
}

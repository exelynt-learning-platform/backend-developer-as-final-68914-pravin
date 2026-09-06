package com.booking.controller;

import com.booking.entity.Reservation;
import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.entity.enums.ReservationStatus;
import com.booking.entity.enums.Role;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import com.booking.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User regularUser;
    private User otherUser;
    private Resource testResource;
    private String adminToken;
    private String userToken;
    private String otherUserToken;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        regularUser = userRepository.save(User.builder()
                .username("user1")
                .email("user1@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        otherUser = userRepository.save(User.builder()
                .username("user2")
                .email("user2@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        testResource = resourceRepository.save(Resource.builder()
                .name("Test Room")
                .type("ROOM")
                .available(true)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateToken(adminUser);
        userToken = "Bearer " + jwtTokenProvider.generateToken(regularUser);
        otherUserToken = "Bearer " + jwtTokenProvider.generateToken(otherUser);
    }

    @Test
    void createReservation_asUser_usesJwtIdentity() throws Exception {
        Map<String, Object> request = Map.of(
                "resourceId", testResource.getId(),
                "startTime", LocalDateTime.now().plusDays(1).toString(),
                "endTime", LocalDateTime.now().plusDays(1).plusHours(2).toString(),
                "price", "100.00"
        );

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(regularUser.getId()))
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getReservations_asAdmin_returnsAllReservations() throws Exception {
        saveReservation(regularUser, testResource);
        saveReservation(otherUser, testResource);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getReservations_asUser_returnsOnlyOwnReservations() throws Exception {
        saveReservation(regularUser, testResource);
        saveReservation(otherUser, testResource);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("user1"));
    }

    @Test
    void getReservationById_asUser_cannotAccessOthersReservation() throws Exception {
        Reservation otherReservation = saveReservation(otherUser, testResource);

        mockMvc.perform(get("/api/reservations/" + otherReservation.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReservationById_asAdmin_canAccessAnyReservation() throws Exception {
        Reservation userReservation = saveReservation(regularUser, testResource);

        mockMvc.perform(get("/api/reservations/" + userReservation.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userReservation.getId()));
    }

    @Test
    void deleteReservation_asUser_returns403() throws Exception {
        Reservation reservation = saveReservation(regularUser, testResource);

        mockMvc.perform(delete("/api/reservations/" + reservation.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReservation_asAdmin_returns204() throws Exception {
        Reservation reservation = saveReservation(regularUser, testResource);

        mockMvc.perform(delete("/api/reservations/" + reservation.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void getReservations_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReservations_filterByStatus_returnsFilteredResults() throws Exception {
        Reservation r1 = saveReservation(regularUser, testResource);
        r1.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(r1);

        saveReservation(regularUser, testResource);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", adminToken)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void getReservations_filterByPrice_returnsFilteredResults() throws Exception {
        Reservation r1 = saveReservation(regularUser, testResource);
        r1.setPrice(new BigDecimal("200.00"));
        reservationRepository.save(r1);

        Reservation r2 = saveReservation(regularUser, testResource);
        r2.setPrice(new BigDecimal("50.00"));
        reservationRepository.save(r2);

        mockMvc.perform(get("/api/reservations")
                        .header("Authorization", adminToken)
                        .param("minPrice", "100.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createReservation_withInvalidDates_returns400() throws Exception {
        Map<String, Object> request = Map.of(
                "resourceId", testResource.getId(),
                "startTime", LocalDateTime.now().plusDays(2).toString(),
                "endTime", LocalDateTime.now().plusDays(1).toString(),
                "price", "100.00"
        );

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private Reservation saveReservation(User user, Resource resource) {
        return reservationRepository.save(Reservation.builder()
                .user(user)
                .resource(resource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build());
    }
}

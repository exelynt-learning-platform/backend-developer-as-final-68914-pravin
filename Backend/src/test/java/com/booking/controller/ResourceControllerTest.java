package com.booking.controller;

import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.entity.enums.Role;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;

    @Autowired
    private com.booking.repository.ReservationRepository reservationRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        User user = userRepository.save(User.builder()
                .username("user1")
                .email("user1@test.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateToken(admin);
        userToken = "Bearer " + jwtTokenProvider.generateToken(user);

        resourceRepository.save(Resource.builder()
                .name("Room A")
                .type("ROOM")
                .available(true)
                .build());
    }

    @Test
    void getResources_asUser_returns200() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getResources_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_asAdmin_returns201() throws Exception {
        Map<String, Object> request = Map.of("name", "New Room", "type", "ROOM");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"));
    }

    @Test
    void createResource_asUser_returns403() throws Exception {
        Map<String, Object> request = Map.of("name", "New Room", "type", "ROOM");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asUser_returns403() throws Exception {
        Resource resource = resourceRepository.findAll().get(0);

        mockMvc.perform(delete("/api/resources/" + resource.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asAdmin_returns204() throws Exception {
        Resource resource = resourceRepository.findAll().get(0);

        mockMvc.perform(delete("/api/resources/" + resource.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void createResource_withMissingFields_returns400() throws Exception {
        Map<String, Object> request = Map.of("name", "Room Without Type");

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
    }

    @Test
    void getResourceById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/resources/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }
}

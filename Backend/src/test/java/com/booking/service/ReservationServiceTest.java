package com.booking.service;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.response.ReservationResponse;
import com.booking.entity.Reservation;
import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.entity.enums.ReservationStatus;
import com.booking.entity.enums.Role;
import com.booking.exception.BadRequestException;
import com.booking.exception.ForbiddenException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.specification.ReservationSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User adminUser;
    private User regularUser;
    private User otherUser;
    private Resource testResource;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        regularUser = User.builder()
                .id(2L)
                .username("user1")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(3L)
                .username("user2")
                .role(Role.USER)
                .build();

        testResource = Resource.builder()
                .id(10L)
                .name("Test Room")
                .type("ROOM")
                .available(true)
                .build();

        testReservation = Reservation.builder()
                .id(100L)
                .user(regularUser)
                .resource(testResource)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .price(new BigDecimal("100.00"))
                .status(ReservationStatus.PENDING)
                .build();
    }

    @Test
    void findById_asAdmin_canAccessAnyReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        ReservationResponse response = reservationService.findById(100L, adminUser);

        assertThat(response.id()).isEqualTo(100L);
    }

    @Test
    void findById_asUser_canAccessOwnReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        ReservationResponse response = reservationService.findById(100L, regularUser);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userId()).isEqualTo(regularUser.getId());
    }

    @Test
    void findById_asUser_cannotAccessOthersReservation() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(testReservation));

        assertThatThrownBy(() -> reservationService.findById(100L, otherUser))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.findById(999L, adminUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_withValidRequest_savesReservationWithJwtUser() {
        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("100.00"),
                "Test notes"
        );

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

        ReservationResponse response = reservationService.create(request, regularUser);

        verify(reservationRepository).save(argThat(r -> r.getUser().getId().equals(regularUser.getId())));
        assertThat(response).isNotNull();
    }

    @Test
    void create_withEndTimeBeforeStartTime_throwsBadRequest() {
        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1),
                new BigDecimal("100.00"),
                null
        );

        assertThatThrownBy(() -> reservationService.create(request, regularUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void create_withUnavailableResource_throwsBadRequest() {
        testResource.setAvailable(false);

        ReservationRequest request = new ReservationRequest(
                10L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                new BigDecimal("100.00"),
                null
        );

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(testResource));

        assertThatThrownBy(() -> reservationService.create(request, regularUser))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void findAll_asAdmin_returnsAllReservations() {
        Page<Reservation> page = new PageImpl<>(List.of(testReservation));
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ReservationResponse> result = reservationService.findAll(null, null, null, PageRequest.of(0, 10), adminUser);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findAll_asUser_filtersToOwnReservations() {
        Page<Reservation> page = new PageImpl<>(List.of(testReservation));
        when(reservationRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<ReservationResponse> result = reservationService.findAll(null, null, null, PageRequest.of(0, 10), regularUser);

        verify(reservationRepository).findAll(any(Specification.class), any(Pageable.class));
        assertThat(result.getContent().get(0).userId()).isEqualTo(regularUser.getId());
    }

    @Test
    void delete_withNonExistentId_throwsResourceNotFoundException() {
        when(reservationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_withValidId_deletesSuccessfully() {
        when(reservationRepository.existsById(100L)).thenReturn(true);

        reservationService.delete(100L);

        verify(reservationRepository).deleteById(100L);
    }
}

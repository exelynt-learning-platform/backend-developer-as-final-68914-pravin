package com.booking.service;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.UpdateReservationRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;

    public Page<ReservationResponse> findAll(ReservationStatus status,
                                              BigDecimal minPrice,
                                              BigDecimal maxPrice,
                                              Pageable pageable,
                                              User currentUser) {
        Long userId = currentUser.getRole() == Role.ADMIN ? null : currentUser.getId();

        Specification<Reservation> spec = ReservationSpecification.buildFilter(userId, status, minPrice, maxPrice);
        return reservationRepository.findAll(spec, pageable).map(ReservationResponse::from);
    }

    public ReservationResponse findById(Long id, User currentUser) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (currentUser.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this reservation");
        }

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request, User currentUser) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Resource resource = resourceRepository.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource", request.resourceId()));

        if (!resource.getAvailable()) {
            throw new BadRequestException("Resource is not available for booking");
        }

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .price(request.price())
                .notes(request.notes())
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public ReservationResponse update(Long id, UpdateReservationRequest request, User currentUser) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (currentUser.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You do not have access to this reservation");
        }

        if (request.startTime() != null && request.endTime() != null
                && !request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        if (request.resourceId() != null) {
            Resource resource = resourceRepository.findById(request.resourceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Resource", request.resourceId()));
            reservation.setResource(resource);
        }

        if (request.startTime() != null) reservation.setStartTime(request.startTime());
        if (request.endTime() != null) reservation.setEndTime(request.endTime());
        if (request.status() != null) reservation.setStatus(request.status());
        if (request.price() != null) reservation.setPrice(request.price());
        if (request.notes() != null) reservation.setNotes(request.notes());

        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation", id);
        }
        reservationRepository.deleteById(id);
    }
}

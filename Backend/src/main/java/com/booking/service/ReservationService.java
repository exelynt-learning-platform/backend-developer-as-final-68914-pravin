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
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final String RESERVATION_NOT_FOUND = "Reservation";
    private static final String RESOURCE_NOT_FOUND = "Resource";

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;

    public Page<ReservationResponse> findAll(
            ReservationStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable,
            User currentUser) {

        Long userId = currentUser.getRole() == Role.ADMIN
                ? null
                : currentUser.getId();

        Specification<Reservation> specification =
                ReservationSpecification.buildFilter(
                        userId,
                        status,
                        minPrice,
                        maxPrice
                );

        return reservationRepository
                .findAll(specification, pageable)
                .map(ReservationResponse::from);
    }

    public ReservationResponse findById(Long id, User currentUser) {
        Reservation reservation = getReservationById(id);

        validateAccess(reservation, currentUser);

        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse create(
            ReservationRequest request,
            User currentUser) {

        validateCreateRequest(request);

        Resource resource = getResourceById(request.resourceId());

        validateResourceAvailability(resource);

        Reservation reservation = Reservation.builder()
                .user(currentUser)
                .resource(resource)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .price(request.price())
                .notes(request.notes())
                .status(ReservationStatus.PENDING)
                .build();

        return ReservationResponse.from(
                reservationRepository.save(reservation)
        );
    }

    @Transactional
    public ReservationResponse update(
            Long id,
            UpdateReservationRequest request,
            User currentUser) {

        Reservation reservation = getReservationById(id);

        validateAccess(reservation, currentUser);
        validateUpdateRequest(request);
        updateResource(reservation, request);
        applyPartialUpdates(reservation, request);

        return ReservationResponse.from(
                reservationRepository.save(reservation)
        );
    }

    @Transactional
    public void delete(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    RESERVATION_NOT_FOUND,
                    id
            );
        }

        reservationRepository.deleteById(id);
    }

    private Reservation getReservationById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESERVATION_NOT_FOUND,
                                id
                        ));
    }

    private Resource getResourceById(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                RESOURCE_NOT_FOUND,
                                resourceId
                        ));
    }

    private void validateAccess(
            Reservation reservation,
            User currentUser) {

        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isOwner = reservation.getUser()
                .getId()
                .equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException(
                    "You do not have access to this reservation"
            );
        }
    }

    private void validateCreateRequest(ReservationRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
    }

    private void validateUpdateRequest(
            UpdateReservationRequest request) {

        if (request.startTime() != null
                && request.endTime() != null
                && !request.endTime().isAfter(request.startTime())) {

            throw new BadRequestException(
                    "End time must be after start time"
            );
        }
    }

    private void validateResourceAvailability(Resource resource) {
        if (!resource.getAvailable()) {
            throw new BadRequestException(
                    "Resource is not available for booking"
            );
        }
    }

    private void updateResource(
            Reservation reservation,
            UpdateReservationRequest request) {

        Optional.ofNullable(request.resourceId())
                .map(this::getResourceById)
                .ifPresent(resource -> {
                    validateResourceAvailability(resource);
                    reservation.setResource(resource);
                });
    }

    private void applyPartialUpdates(
            Reservation reservation,
            UpdateReservationRequest request) {

        applyIfPresent(
                request.startTime(),
                reservation::setStartTime
        );

        applyIfPresent(
                request.endTime(),
                reservation::setEndTime
        );

        applyIfPresent(
                request.status(),
                reservation::setStatus
        );

        applyIfPresent(
                request.price(),
                reservation::setPrice
        );

        applyIfPresent(
                request.notes(),
                reservation::setNotes
        );
    }

    private <T> void applyIfPresent(
            T value,
            Consumer<T> updater) {

        Optional.ofNullable(value)
                .ifPresent(updater);
    }
}
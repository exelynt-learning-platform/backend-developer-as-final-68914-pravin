package com.booking.controller;

import com.booking.dto.request.ReservationRequest;
import com.booking.dto.request.UpdateReservationRequest;
import com.booking.dto.response.ReservationResponse;
import com.booking.entity.User;
import com.booking.entity.enums.ReservationStatus;
import com.booking.exception.BadRequestException;
import com.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Set;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Endpoints for managing reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_SIZE = "10";
    private static final String DEFAULT_SORT_BY = "createdAt";
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id",
            "price",
            "startTime",
            "createdAt"
    );

    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "List reservations with optional filtering, pagination, and sorting")
    public ResponseEntity<Page<ReservationResponse>> getAll(
            @Parameter(description = "Filter by status")
            @RequestParam(required = false) ReservationStatus status,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @Parameter(description = "Sort field (e.g. id, price, startTime, createdAt)")
            @RequestParam(defaultValue = DEFAULT_SORT_BY) String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION) Direction sortDir,
            @AuthenticationPrincipal User currentUser) {

        Pageable pageable = createPageable(page, size, sortBy, sortDir);

        return ResponseEntity.ok(
                reservationService.findAll(
                        status,
                        minPrice,
                        maxPrice,
                        pageable,
                        currentUser
                )
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by ID")
    public ResponseEntity<ReservationResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reservationService.findById(id, currentUser)
        );
    }

    @PostMapping
    @Operation(summary = "Create a new reservation")
    public ResponseEntity<ReservationResponse> create(
            @Valid @RequestBody ReservationRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationService.create(request, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReservationRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ResponseEntity.ok(
                reservationService.update(id, request, currentUser)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a reservation (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {

        reservationService.delete(id);

        return ResponseEntity.noContent().build();
    }

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            Direction sortDir) {

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException(
                    "Invalid sortBy field. Allowed values are: "
                            + String.join(", ", ALLOWED_SORT_FIELDS)
            );
        }

        Sort sort = Sort.by(sortDir, sortBy);

        return PageRequest.of(page, size, sort);
    }
}
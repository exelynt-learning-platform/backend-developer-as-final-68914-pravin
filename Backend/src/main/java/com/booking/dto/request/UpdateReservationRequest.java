package com.booking.dto.request;

import com.booking.entity.enums.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateReservationRequest(
        Long resourceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        ReservationStatus status,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        BigDecimal price,

        String notes
) {
}

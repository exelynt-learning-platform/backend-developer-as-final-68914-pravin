package com.booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ResourceRequest(
        @NotBlank(message = "Resource name is required")
        String name,

        String description,

        @NotBlank(message = "Resource type is required")
        String type,

        @Positive(message = "Capacity must be a positive number")
        Integer capacity,

        String location,

        Boolean available
) {
}

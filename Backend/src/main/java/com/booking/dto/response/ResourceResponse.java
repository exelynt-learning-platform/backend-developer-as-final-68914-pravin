package com.booking.dto.response;

import com.booking.entity.Resource;

import java.time.LocalDateTime;

public record ResourceResponse(
        Long id,
        String name,
        String description,
        String type,
        Integer capacity,
        String location,
        Boolean available,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getCapacity(),
                resource.getLocation(),
                resource.getAvailable(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}

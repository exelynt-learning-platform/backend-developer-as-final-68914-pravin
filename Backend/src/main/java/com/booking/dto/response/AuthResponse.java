package com.booking.dto.response;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String role
) {
    public AuthResponse(String token, String username, String role) {
        this(token, "Bearer", username, role);
    }
}

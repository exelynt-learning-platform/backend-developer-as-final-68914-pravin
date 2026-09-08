package com.booking.service;

import com.booking.dto.request.LoginRequest;
import com.booking.dto.response.AuthResponse;
import com.booking.entity.User;
import com.booking.repository.UserRepository;
import com.booking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow();

        String token = jwtTokenProvider.generateToken(user);

        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}

package com.booking;

import com.booking.entity.Reservation;
import com.booking.entity.Resource;
import com.booking.entity.User;
import com.booking.entity.enums.ReservationStatus;
import com.booking.entity.enums.Role;
import com.booking.repository.ReservationRepository;
import com.booking.repository.ResourceRepository;
import com.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@booking.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .build());

        User user = userRepository.save(User.builder()
                .username("john")
                .email("john@booking.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        userRepository.save(User.builder()
                .username("jane")
                .email("jane@booking.com")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .build());

        Resource room1 = resourceRepository.save(Resource.builder()
                .name("Conference Room A")
                .description("Large conference room with projector and whiteboard")
                .type("ROOM")
                .capacity(20)
                .location("Building 1, Floor 2")
                .available(true)
                .build());

        Resource room2 = resourceRepository.save(Resource.builder()
                .name("Meeting Room B")
                .description("Small meeting room suitable for 1-on-1s")
                .type("ROOM")
                .capacity(6)
                .location("Building 1, Floor 3")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Company Van")
                .description("7-seater company van")
                .type("VEHICLE")
                .capacity(7)
                .location("Parking Lot B")
                .available(true)
                .build());

        resourceRepository.save(Resource.builder()
                .name("Projector")
                .description("Portable HD projector")
                .type("EQUIPMENT")
                .capacity(1)
                .location("Storage Room 1")
                .available(true)
                .build());

        reservationRepository.save(Reservation.builder()
                .user(user)
                .resource(room1)
                .startTime(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0))
                .price(new BigDecimal("150.00"))
                .status(ReservationStatus.CONFIRMED)
                .notes("Team standup meeting")
                .build());

        reservationRepository.save(Reservation.builder()
                .user(user)
                .resource(room2)
                .startTime(LocalDateTime.now().plusDays(2).withHour(14).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(2).withHour(15).withMinute(0))
                .price(new BigDecimal("50.00"))
                .status(ReservationStatus.PENDING)
                .notes("1-on-1 with manager")
                .build());
    }
}

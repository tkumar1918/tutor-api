package dev.tushar.tutorapi.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        boolean admin,
        Instant createdAt,
        Instant updatedAt) {}

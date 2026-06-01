package dev.tushar.tutorapi.dto.response;

import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
import java.time.Instant;

public record EnrollmentResponse(
        Long id,
        Long userId,
        String userName,
        Long courseId,
        String courseTitle,
        Instant enrolledAt,
        EnrollmentStatus status) {}

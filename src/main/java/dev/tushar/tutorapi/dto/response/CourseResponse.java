package dev.tushar.tutorapi.dto.response;

import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import java.time.Instant;

public record CourseResponse(
        Long id,
        String title,
        String description,
        Subject subject,
        Level level,
        Long priceCents,
        Long tutorId,
        String tutorName,
        Instant createdAt,
        Instant updatedAt) {}

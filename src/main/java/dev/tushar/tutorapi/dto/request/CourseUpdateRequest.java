package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CourseUpdateRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        Subject subject,
        Level level,
        @PositiveOrZero Long priceCents) {}

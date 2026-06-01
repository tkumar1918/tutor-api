package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.Expertise;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Edits to a tutor profile — name lives on User, not here. */
public record TutorUpdateRequest(
        @Size(max = 1000) String bio,
        Expertise expertise,
        @Size(max = 500) String qualifications,
        @PositiveOrZero @Max(60) Integer yearsOfExperience,
        @PositiveOrZero Long hourlyRateCents) {}

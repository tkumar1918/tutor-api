package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.Expertise;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Body for "I want to become a tutor" (POST /api/v1/me/tutor-application). */
public record TutorApplicationRequest(
        @Size(max = 1000) String bio,
        @NotNull Expertise expertise,
        @Size(max = 500) String qualifications,
        @NotNull @PositiveOrZero @Max(60) Integer yearsOfExperience,
        @NotNull @PositiveOrZero Long hourlyRateCents) {}

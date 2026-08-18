package dev.tushar.tutorapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for "rate this tutor" (POST /api/v1/tutors/{tutorId}/reviews). */
public record ReviewCreateRequest(
        @NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {}

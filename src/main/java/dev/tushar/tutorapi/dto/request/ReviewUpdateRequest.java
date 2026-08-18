package dev.tushar.tutorapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for editing your own review (PATCH /api/v1/reviews/{id}). */
public record ReviewUpdateRequest(
        @NotNull @Min(1) @Max(5) Integer rating, @Size(max = 1000) String comment) {}

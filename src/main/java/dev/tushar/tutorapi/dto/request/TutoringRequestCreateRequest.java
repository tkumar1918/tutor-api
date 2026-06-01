package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.Subject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for "I'd like 1:1 sessions with this tutor" (POST /api/v1/tutoring-requests). */
public record TutoringRequestCreateRequest(
        @NotNull Long tutorId,
        @NotNull Subject subject,
        @NotBlank @Size(max = 1000) String message) {}

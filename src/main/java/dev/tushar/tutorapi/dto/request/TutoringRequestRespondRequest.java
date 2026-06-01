package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for "I'll take this on / I can't take this on" (PATCH /api/v1/tutoring-requests/{id}).
 * {@code status} must be ACCEPTED or REJECTED — anything else is rejected by the service.
 */
public record TutoringRequestRespondRequest(
        @NotNull TutoringRequestStatus status,
        @Size(max = 1000) String tutorReply) {}

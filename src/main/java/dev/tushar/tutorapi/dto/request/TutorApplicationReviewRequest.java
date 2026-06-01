package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin decision on a tutor application. {@code status} must be APPROVED or REJECTED.
 * {@code rejectionReason} is required when status = REJECTED.
 */
public record TutorApplicationReviewRequest(
        @NotNull TutorApplicationStatus status,
        @Size(max = 500) String rejectionReason) {}

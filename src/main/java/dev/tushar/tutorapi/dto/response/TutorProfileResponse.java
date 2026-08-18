package dev.tushar.tutorapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import java.time.Instant;

// Public catalog payload. Email is intentionally excluded — the catalog is unauthenticated
// and exposing tutor email here would be a PII leak. Use an auth-gated contact endpoint or
// the /me/* endpoints if the tutor needs to see their own email.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TutorProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String bio,
        Expertise expertise,
        String qualifications,
        int yearsOfExperience,
        long hourlyRateCents,
        TutorApplicationStatus status,
        Instant appliedAt,
        Instant reviewedAt,
        String rejectionReason,
        // Catalog rating, aggregated from reviews. averageRating is null (omitted) until the
        // tutor has at least one review; reviewCount is then 0.
        Double averageRating,
        long reviewCount,
        Instant createdAt,
        Instant updatedAt) {}

package dev.tushar.tutorapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;

/**
 * Identity payload for {@code GET /api/v1/me}. {@code tutorProfile} is present only when the
 * authenticated user has applied to become a tutor (regardless of approval status — the UI
 * can show "Your application is pending" vs "You are an approved tutor").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CurrentUserResponse(
        UserResponse user,
        Set<String> authorities,
        TutorProfileResponse tutorProfile) {}

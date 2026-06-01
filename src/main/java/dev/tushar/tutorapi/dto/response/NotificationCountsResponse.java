package dev.tushar.tutorapi.dto.response;

/**
 * Aggregate counts of items that need the caller's action right now. Returned by
 * {@code GET /api/v1/me/notifications}. Both fields are 0 when not applicable to the
 * caller's role (e.g. a regular user sees 0/0). The frontend renders a badge on a nav
 * item only when the matching count is greater than zero.
 *
 * <p>This is intentionally <strong>not</strong> a real notification system: there is no
 * read/unread tracking. A counter goes down when the underlying item leaves the
 * PENDING state (tutor responds, admin reviews) — not when the caller "sees" it.
 */
public record NotificationCountsResponse(
        long tutorPendingRequests,
        long adminPendingApplications) {}

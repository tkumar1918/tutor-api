package dev.tushar.tutorapi.entity.enums;

/**
 * Lifecycle of a 1:1 tutoring request:
 * <ul>
 *   <li>{@code PENDING}  — student submitted; tutor hasn't responded yet.</li>
 *   <li>{@code ACCEPTED} — tutor agreed; the two arrange the session out-of-band.</li>
 *   <li>{@code REJECTED} — tutor declined (with an optional reply note).</li>
 *   <li>{@code CANCELLED} — student withdrew while still PENDING.</li>
 * </ul>
 * Only PENDING rows are mutable; the other three are terminal.
 */
public enum TutoringRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED
}

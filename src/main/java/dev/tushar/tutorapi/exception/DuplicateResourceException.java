package dev.tushar.tutorapi.exception;

/**
 * Thrown when a unique constraint would be violated (duplicate username, email, enrollment,
 * tutor application, …). Mapped to HTTP 409. Prefer this over letting Spring surface the
 * raw {@code DataIntegrityViolationException} so the message stays user-friendly.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}

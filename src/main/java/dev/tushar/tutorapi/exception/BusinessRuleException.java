package dev.tushar.tutorapi.exception;

/**
 * Thrown when a request is syntactically valid but violates a domain invariant — e.g.
 * reviewing an already-reviewed application, or accepting an already-accepted tutoring
 * request. Mapped to HTTP 422 (Unprocessable Content).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

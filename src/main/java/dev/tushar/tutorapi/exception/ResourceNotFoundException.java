package dev.tushar.tutorapi.exception;

/** Thrown when an id (or other natural key) doesn't resolve. Mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s not found with id: %s".formatted(resource, id));
    }
}

package dev.tushar.tutorapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Uniform error payload. {@code code} is a stable machine-readable discriminator the
 * frontend can switch on (e.g. show different toasts for {@code TOKEN_EXPIRED} vs
 * {@code INVALID_CREDENTIALS}). It is intentionally optional — only emitted when the
 * server has more specific info than the HTTP status alone conveys.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, null, message, path, null);
    }

    public static ErrorResponse of(
            int status, String error, String code, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, code, message, path, null);
    }

    public static ErrorResponse of(
            int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, null, message, path, fieldErrors);
    }

    public record FieldError(String field, String message, Object rejectedValue) {}
}

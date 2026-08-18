package dev.tushar.tutorapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewResponse(
        Long id,
        Long tutorId,
        Long studentId,
        String studentName,
        int rating,
        String comment,
        Instant createdAt,
        Instant updatedAt) {}

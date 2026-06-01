package dev.tushar.tutorapi.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.tushar.tutorapi.entity.enums.Subject;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TutoringRequestResponse(
        Long id,
        Long studentId,
        String studentName,
        Long tutorId,
        String tutorName,
        Subject subject,
        String message,
        TutoringRequestStatus status,
        String tutorReply,
        Instant createdAt,
        Instant respondedAt) {}

package dev.tushar.tutorapi.dto.request;

import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record EnrollmentStatusUpdateRequest(@NotNull EnrollmentStatus status) {}

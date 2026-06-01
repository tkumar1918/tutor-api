package dev.tushar.tutorapi.dto.request;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Update one's own basic profile (name + DOB). Email/username are immutable here. */
public record UserUpdateRequest(
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,
        @Past LocalDate dateOfBirth) {}

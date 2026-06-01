package dev.tushar.tutorapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The single identity row for every human in the system. Authorities are not stored here —
 * they're derived at auth time from {@link #admin} plus the optional {@link TutorProfile}
 * link (see {@code CustomUserDetailsService}). Username and email are both globally unique.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
            @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * Elevated user. STUDENT capability is implicit (any authenticated user can enroll).
     * TUTOR capability comes from an APPROVED {@code TutorProfile}. ADMIN is this flag.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean admin = false;

    /**
     * Server-side "session epoch." Incremented whenever the user's authorities change
     * (e.g. their tutor application is APPROVED). The current value is embedded in every
     * JWT as the {@code tv} claim; the auth filter rejects any JWT whose {@code tv}
     * does not match the live DB value. Result: a single SQL update invalidates every
     * outstanding JWT for that user, forcing re-login.
     */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private long tokenVersion = 0L;

    /** Convenience for "bump and force re-login on next request." */
    public void bumpTokenVersion() {
        this.tokenVersion++;
    }
}

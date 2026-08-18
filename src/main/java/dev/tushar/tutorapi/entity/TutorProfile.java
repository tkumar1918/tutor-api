package dev.tushar.tutorapi.entity;

import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tutor capability attached to a {@link User}. Created when the user APPLIES to become a tutor.
 * Only rows with {@code status = APPROVED} are visible in the public catalog.
 */
@Entity
@Table(
        name = "tutor_profiles",
        uniqueConstraints = @UniqueConstraint(name = "uk_tutor_profiles_user", columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 1000)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Expertise expertise;

    // Free-form qualifications blurb — degrees, certifications, where they teach.
    // Deliberately one text field, not a separate Qualification table, because this is a
    // learner project and a real product would replace it with verified credentials anyway.
    @Column(length = 500)
    private String qualifications;

    @Column(name = "years_of_experience", nullable = false)
    private int yearsOfExperience;

    // Stored as integer cents — avoids float/BigDecimal-as-number serialization ambiguity in JSON.
    @Column(name = "hourly_rate_cents", nullable = false)
    private long hourlyRateCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TutorApplicationStatus status;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}

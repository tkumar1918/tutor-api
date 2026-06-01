package dev.tushar.tutorapi.entity;

import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A packaged course offered by an APPROVED {@link TutorProfile}. Learners join via
 * {@link Enrollment}. The course's {@code priceCents} is independent of the tutor's
 * {@link TutorProfile#getHourlyRateCents() hourlyRateCents} — one is a flat fee per
 * course, the other is the hourly rate for 1:1 {@link TutoringRequest} sessions.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Level level;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutor;
}

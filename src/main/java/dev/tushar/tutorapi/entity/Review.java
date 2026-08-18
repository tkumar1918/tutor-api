package dev.tushar.tutorapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student's public rating of a tutor. Eligibility is enforced in the service: the student
 * must have had at least one {@code ACCEPTED} {@link TutoringRequest} with the tutor. The
 * unique constraint caps it at one review per student–tutor pair — re-reviewing edits the
 * existing row rather than stacking duplicates.
 */
@Entity
@Table(
        name = "reviews",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_reviews_tutor_student",
                        columnNames = {"tutor_profile_id", "student_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    /** Whole stars, 1–5. Bounds are validated on the request DTO. */
    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;
}

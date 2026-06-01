package dev.tushar.tutorapi.entity;

import dev.tushar.tutorapi.entity.enums.Subject;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A student's 1:1 tutoring inquiry sent to an APPROVED tutor. Independent of {@link Course} —
 * this is the "book this tutor for private sessions" side of the marketplace. The tutor's
 * {@link TutorProfile#getHourlyRateCents() hourly rate} is the implied price.
 */
@Entity
@Table(name = "tutoring_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutoringRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tutor_profile_id", nullable = false)
    private TutorProfile tutor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Subject subject;

    /** What the student wants to learn / availability — free-form. */
    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TutoringRequestStatus status;

    /** Tutor's note when accepting or rejecting. Null while PENDING / on student-cancel. */
    @Column(name = "tutor_reply", length = 1000)
    private String tutorReply;

    @Column(name = "responded_at")
    private Instant respondedAt;
}

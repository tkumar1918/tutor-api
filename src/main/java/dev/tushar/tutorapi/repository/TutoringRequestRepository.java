package dev.tushar.tutorapi.repository;

import dev.tushar.tutorapi.entity.TutoringRequest;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * {@link TutoringRequest} reads. Same "one filterable search + small count helpers" shape as
 * {@code EnrollmentRepository} — keeps the controller surface paginated and predictable.
 */
@Repository
public interface TutoringRequestRepository extends JpaRepository<TutoringRequest, Long> {

    // Mapper walks r → student and r → tutor → user, so prefetch the whole chain (open-in-view
    // is off; lazy loads outside this tx would fail).
    @EntityGraph(attributePaths = {"student", "tutor", "tutor.user"})
    @Query(
            """
            SELECT r FROM TutoringRequest r
            WHERE (:studentId IS NULL OR r.student.id = :studentId)
              AND (:tutorId   IS NULL OR r.tutor.id   = :tutorId)
              AND (:status    IS NULL OR r.status     = :status)
            """)
    Page<TutoringRequest> search(
            @Param("studentId") Long studentId,
            @Param("tutorId") Long tutorId,
            @Param("status") TutoringRequestStatus status,
            Pageable pageable);

    long countByTutorIdAndStatus(Long tutorId, TutoringRequestStatus status);
}

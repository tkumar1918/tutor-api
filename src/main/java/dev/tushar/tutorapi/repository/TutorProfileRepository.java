package dev.tushar.tutorapi.repository;

import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * {@link TutorProfile} lookups. The catalog search ({@link #searchApproved}) hard-filters to
 * APPROVED rows in JPQL so callers never accidentally surface PENDING/REJECTED profiles.
 */
@Repository
public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<TutorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    long countByStatus(TutorApplicationStatus status);

    @EntityGraph(attributePaths = "user")
    Page<TutorProfile> findByStatus(TutorApplicationStatus status, Pageable pageable);

    /** Public catalog: only APPROVED tutors are visible, optionally filtered. */
    @EntityGraph(attributePaths = "user")
    @Query(
            """
            SELECT t FROM TutorProfile t
            WHERE t.status = dev.tushar.tutorapi.entity.enums.TutorApplicationStatus.APPROVED
              AND (:expertise IS NULL OR t.expertise = :expertise)
              AND (:search    IS NULL OR LOWER(t.user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                                      OR LOWER(t.user.lastName)  LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<TutorProfile> searchApproved(
            @Param("expertise") Expertise expertise,
            @Param("search") String search,
            Pageable pageable);
}

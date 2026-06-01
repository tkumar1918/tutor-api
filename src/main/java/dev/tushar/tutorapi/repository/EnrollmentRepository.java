package dev.tushar.tutorapi.repository;

import dev.tushar.tutorapi.entity.Enrollment;
import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * {@link Enrollment} reads. {@link #search} is one query that powers four endpoints
 * (admin list, my-enrollments, course's-students, ad-hoc filters) via nullable params —
 * the standard "optional filter" JPQL idiom.
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    // The mapper walks e → course → tutor → user (userName), so prefetch the whole chain
    // to avoid N+1 across paginated results. open-in-view=false means lazy loads outside
    // this tx would also blow up — so this is correctness, not just perf.
    @EntityGraph(attributePaths = {"user", "course", "course.tutor", "course.tutor.user"})
    @Query(
            """
            SELECT e FROM Enrollment e
            WHERE (:userId   IS NULL OR e.user.id    = :userId)
              AND (:courseId IS NULL OR e.course.id  = :courseId)
              AND (:status   IS NULL OR e.status     = :status)
            """)
    Page<Enrollment> search(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentStatus status,
            Pageable pageable);
}

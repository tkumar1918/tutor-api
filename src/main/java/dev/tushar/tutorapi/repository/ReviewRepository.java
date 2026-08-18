package dev.tushar.tutorapi.repository;

import dev.tushar.tutorapi.entity.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * {@link Review} reads. The public listing prefetches the {@code student} (the mapper renders
 * the reviewer's name) and {@link #ratingSummaries} batch-aggregates ratings for a page of
 * tutors so the catalog avoids an N+1.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByTutorIdAndStudentId(Long tutorId, Long studentId);

    @EntityGraph(attributePaths = "student")
    Page<Review> findByTutorId(Long tutorId, Pageable pageable);

    /** One row per tutor that has at least one review; tutors with none are simply absent. */
    @Query(
            """
            SELECT r.tutor.id AS tutorId, AVG(r.rating) AS averageRating, COUNT(r) AS reviewCount
            FROM Review r
            WHERE r.tutor.id IN :tutorIds
            GROUP BY r.tutor.id
            """)
    List<TutorRatingProjection> ratingSummaries(@Param("tutorIds") Collection<Long> tutorIds);
}

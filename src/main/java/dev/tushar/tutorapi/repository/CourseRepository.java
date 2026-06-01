package dev.tushar.tutorapi.repository;

import dev.tushar.tutorapi.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * {@link Course} reads. Extends {@link JpaSpecificationExecutor} so the public list endpoint
 * can compose criteria filters via {@code CourseSpecifications}.
 */
@Repository
public interface CourseRepository
        extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Page<Course> findByTutorId(Long tutorId, Pageable pageable);
}

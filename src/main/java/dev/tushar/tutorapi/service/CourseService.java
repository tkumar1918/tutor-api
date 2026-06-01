package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.CourseCreateRequest;
import dev.tushar.tutorapi.dto.request.CourseUpdateRequest;
import dev.tushar.tutorapi.dto.response.CourseResponse;
import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Course CRUD seen by controllers. Implemented by {@link CourseServiceImpl}. Mutating ops
 * verify the caller is the owning tutor (or admin); reads are public for the catalog.
 *
 * <p>This is intentionally an interface to demonstrate the "service interface + impl" pattern
 * even though there's only one implementation — keeps the tutorial honest.
 */
public interface CourseService {

    CourseResponse createForCurrentTutor(CourseCreateRequest request);

    CourseResponse getById(Long id);

    Page<CourseResponse> list(Long tutorId, Subject subject, Level level, String search, Pageable pageable);

    Page<CourseResponse> listByTutor(Long tutorId, Pageable pageable);

    Page<CourseResponse> listForCurrentTutor(Pageable pageable);

    CourseResponse update(Long id, CourseUpdateRequest request);

    void delete(Long id);
}

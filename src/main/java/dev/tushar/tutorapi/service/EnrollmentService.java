package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.EnrollmentCreateRequest;
import dev.tushar.tutorapi.dto.request.EnrollmentStatusUpdateRequest;
import dev.tushar.tutorapi.dto.response.EnrollmentResponse;
import dev.tushar.tutorapi.entity.Course;
import dev.tushar.tutorapi.entity.Enrollment;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
import dev.tushar.tutorapi.exception.DuplicateResourceException;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.EnrollmentMapper;
import dev.tushar.tutorapi.repository.CourseRepository;
import dev.tushar.tutorapi.repository.EnrollmentRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD + permission logic for {@link Enrollment}. The current user is always the enrolled
 * party — there is no {@code userId} field in any request body. Visibility / mutation gates:
 *
 * <ul>
 *   <li>Enroll: any authenticated user, once per course (DB uniqueness backs this up).</li>
 *   <li>View one: the enrolled user, the course's tutor, or admin.</li>
 *   <li>Status update / delete: only the enrolled user, or admin. Tutors cannot drop students.</li>
 *   <li>{@link #listForCourse} — the "my students" view for the tutor who owns the course.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public EnrollmentResponse enrollCurrentUser(EnrollmentCreateRequest request) {
        User user = currentUserService.user();
        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), request.courseId())) {
            throw new DuplicateResourceException(
                    "You are already enrolled in course " + request.courseId());
        }
        Course course = courseRepository
                .findById(request.courseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Course", request.courseId()));

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .enrolledAt(Instant.now())
                .status(EnrollmentStatus.ACTIVE)
                .build();
        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getById(Long id) {
        Enrollment enrollment = findOrThrow(id);
        assertCanView(enrollment);
        return enrollmentMapper.toResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> list(
            Long userId, Long courseId, EnrollmentStatus status, Pageable pageable) {
        return enrollmentRepository
                .search(userId, courseId, status, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listForCurrentUser(EnrollmentStatus status, Pageable pageable) {
        User user = currentUserService.user();
        return enrollmentRepository
                .search(user.getId(), null, status, pageable)
                .map(enrollmentMapper::toResponse);
    }

    /**
     * Enrollments on a specific course — for the tutor's "my students" view.
     * Caller must own the course (or be admin); otherwise 403.
     */
    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> listForCourse(
            Long courseId, EnrollmentStatus status, Pageable pageable) {
        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", courseId));
        User user = currentUserService.user();
        boolean isAdmin = user.isAdmin();
        boolean isOwningTutor = course.getTutor().getUser().getId().equals(user.getId());
        if (!isAdmin && !isOwningTutor) {
            throw new AccessDeniedException("You don't own this course");
        }
        return enrollmentRepository
                .search(null, courseId, status, pageable)
                .map(enrollmentMapper::toResponse);
    }

    @Transactional
    public EnrollmentResponse updateStatus(Long id, EnrollmentStatusUpdateRequest request) {
        Enrollment enrollment = findOrThrow(id);
        assertCanModify(enrollment);
        enrollment.setStatus(request.status());
        return enrollmentMapper.toResponse(enrollment);
    }

    @Transactional
    public void delete(Long id) {
        Enrollment enrollment = findOrThrow(id);
        assertCanModify(enrollment);
        enrollmentRepository.delete(enrollment);
    }

    private void assertCanView(Enrollment e) {
        User user = currentUserService.user();
        if (user.isAdmin())
            return;
        if (e.getUser().getId().equals(user.getId()))
            return;
        if (e.getCourse().getTutor().getUser().getId().equals(user.getId()))
            return;
        throw new AccessDeniedException("Not your enrollment");
    }

    private void assertCanModify(Enrollment e) {
        User user = currentUserService.user();
        if (user.isAdmin())
            return;
        if (e.getUser().getId().equals(user.getId()))
            return;
        throw new AccessDeniedException("You can only modify your own enrollments");
    }

    private Enrollment findOrThrow(Long id) {
        return enrollmentRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Enrollment", id));
    }
}

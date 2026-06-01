package dev.tushar.tutorapi.service;

import static dev.tushar.tutorapi.specification.CourseSpecifications.*;

import dev.tushar.tutorapi.dto.request.CourseCreateRequest;
import dev.tushar.tutorapi.dto.request.CourseUpdateRequest;
import dev.tushar.tutorapi.dto.response.CourseResponse;
import dev.tushar.tutorapi.entity.Course;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.CourseMapper;
import dev.tushar.tutorapi.repository.CourseRepository;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CourseService}. The public list query uses
 * {@code CourseSpecifications}
 * to compose JPA criteria for the optional
 * {@code tutorId / subject / level / search}
 * filters — a small showcase of {@code JpaSpecificationExecutor}.
 */
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final CourseMapper courseMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public CourseResponse createForCurrentTutor(CourseCreateRequest request) {
        TutorProfile tutor = currentUserService.approvedTutorProfile();
        Course course = Course.builder()
                .title(request.title())
                .description(request.description())
                .subject(request.subject())
                .level(request.level())
                .priceCents(request.priceCents())
                .tutor(tutor)
                .build();
        return courseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        return courseMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> list(
            Long tutorId, Subject subject, Level level, String search, Pageable pageable) {
        Specification<Course> spec = Specification.allOf(
                hasTutor(tutorId), hasSubject(subject), hasLevel(level), titleContains(search));
        return courseRepository.findAll(spec, pageable).map(courseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> listByTutor(Long tutorId, Pageable pageable) {
        if (!tutorProfileRepository.existsById(tutorId)) {
            throw ResourceNotFoundException.of("Tutor", tutorId);
        }
        return courseRepository.findByTutorId(tutorId, pageable).map(courseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> listForCurrentTutor(Pageable pageable) {
        TutorProfile tutor = currentUserService.approvedTutorProfile();
        return courseRepository.findByTutorId(tutor.getId(), pageable).map(courseMapper::toResponse);
    }

    @Override
    @Transactional
    public CourseResponse update(Long id, CourseUpdateRequest request) {
        Course course = findOrThrow(id);
        assertCanModify(course);
        courseMapper.updateEntity(request, course);
        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Course course = findOrThrow(id);
        assertCanModify(course);
        courseRepository.delete(course);
    }

    private void assertCanModify(Course course) {
        User user = currentUserService.user();
        if (user.isAdmin())
            return;
        TutorProfile owner = course.getTutor();
        if (!owner.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't own this course");
        }
        /*
         * Owner check passes, but if the tutor profile was unapproved in the meantime
         * (PENDING after re-application, or REJECTED), they shouldn't be editing
         * courses.
         */
        if (owner.getStatus() != TutorApplicationStatus.APPROVED) {
            throw new AccessDeniedException(
                    "Your tutor profile is " + owner.getStatus().name() + "; cannot modify courses");
        }
    }

    private Course findOrThrow(Long id) {
        return courseRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Course", id));
    }
}

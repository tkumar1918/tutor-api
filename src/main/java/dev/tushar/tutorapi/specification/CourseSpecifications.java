package dev.tushar.tutorapi.specification;

import dev.tushar.tutorapi.entity.Course;
import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable JPA {@link Specification}s for the public course list endpoint. Each helper
 * returns a no-op predicate when its argument is null, so callers can {@code allOf(...)}
 * them all and JPA filters only by whatever the client actually provided.
 */
public final class CourseSpecifications {

    private CourseSpecifications() {}

    public static Specification<Course> hasTutor(Long tutorId) {
        return (root, query, cb) ->
                tutorId == null ? cb.conjunction() : cb.equal(root.get("tutor").get("id"), tutorId);
    }

    public static Specification<Course> hasSubject(Subject subject) {
        return (root, query, cb) ->
                subject == null ? cb.conjunction() : cb.equal(root.get("subject"), subject);
    }

    public static Specification<Course> hasLevel(Level level) {
        return (root, query, cb) ->
                level == null ? cb.conjunction() : cb.equal(root.get("level"), level);
    }

    public static Specification<Course> titleContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%");
        };
    }
}

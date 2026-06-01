package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.response.EnrollmentResponse;
import dev.tushar.tutorapi.entity.Enrollment;
import dev.tushar.tutorapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface EnrollmentMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user", qualifiedByName = "userFullName")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    EnrollmentResponse toResponse(Enrollment enrollment);

    @Named("userFullName")
    default String userFullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}

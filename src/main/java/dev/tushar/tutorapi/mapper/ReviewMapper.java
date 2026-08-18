package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.response.ReviewResponse;
import dev.tushar.tutorapi.entity.Review;
import dev.tushar.tutorapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface ReviewMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student", qualifiedByName = "fullName")
    ReviewResponse toResponse(Review review);

    @Named("fullName")
    default String fullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}

package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.response.TutoringRequestResponse;
import dev.tushar.tutorapi.entity.TutoringRequest;
import dev.tushar.tutorapi.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface TutoringRequestMapper {

    @Mapping(target = "studentId", source = "student.id")
    @Mapping(target = "studentName", source = "student", qualifiedByName = "fullName")
    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", source = "tutor.user", qualifiedByName = "fullName")
    TutoringRequestResponse toResponse(TutoringRequest request);

    @Named("fullName")
    default String fullName(User user) {
        return user == null ? null : user.getFirstName() + " " + user.getLastName();
    }
}

package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.request.CourseUpdateRequest;
import dev.tushar.tutorapi.dto.response.CourseResponse;
import dev.tushar.tutorapi.entity.Course;
import dev.tushar.tutorapi.entity.TutorProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface CourseMapper {

    @Mapping(target = "tutorId", source = "tutor.id")
    @Mapping(target = "tutorName", source = "tutor", qualifiedByName = "tutorFullName")
    CourseResponse toResponse(Course course);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(CourseUpdateRequest request, @MappingTarget Course course);

    @Named("tutorFullName")
    default String tutorFullName(TutorProfile tutor) {
        if (tutor == null || tutor.getUser() == null) return null;
        return tutor.getUser().getFirstName() + " " + tutor.getUser().getLastName();
    }
}

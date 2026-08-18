package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.request.TutorUpdateRequest;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface TutorProfileMapper {

    // Plain mapping for contexts that don't carry a rating (own-profile view, the apply/review
    // lifecycle): averageRating stays null and reviewCount 0.
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    TutorProfileResponse toResponse(TutorProfile profile);

    // Catalog mapping: averageRating / reviewCount are aggregated from reviews by the service
    // and mapped onto the like-named target fields; everything else comes from the profile.
    @Mapping(target = "userId", source = "profile.user.id")
    @Mapping(target = "firstName", source = "profile.user.firstName")
    @Mapping(target = "lastName", source = "profile.user.lastName")
    TutorProfileResponse toResponse(TutorProfile profile, Double averageRating, long reviewCount);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TutorUpdateRequest request, @MappingTarget TutorProfile profile);
}

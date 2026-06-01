package dev.tushar.tutorapi.mapper;

import dev.tushar.tutorapi.dto.request.UserUpdateRequest;
import dev.tushar.tutorapi.dto.response.UserResponse;
import dev.tushar.tutorapi.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserUpdateRequest request, @MappingTarget User user);
}

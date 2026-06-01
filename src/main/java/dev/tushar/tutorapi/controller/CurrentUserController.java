package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.UserUpdateRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.CurrentUserResponse;
import dev.tushar.tutorapi.dto.response.NotificationCountsResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.mapper.TutorProfileMapper;
import dev.tushar.tutorapi.mapper.UserMapper;
import dev.tushar.tutorapi.security.CurrentUserService;
import dev.tushar.tutorapi.security.CustomUserDetailsService;
import dev.tushar.tutorapi.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me", description = "Current authenticated user + linked profile")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService currentUserService;
    private final CustomUserDetailsService customUserDetailsService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final TutorProfileMapper tutorProfileMapper;

    @Operation(summary = "Get current user + computed authorities + optional tutor profile")
    @GetMapping
    public ApiResponse<CurrentUserResponse> currentUser() {
        return ApiResponse.ok(buildResponse(currentUserService.user()));
    }

    @Operation(summary = "Counts of items needing my action (PENDING tutor inbox / PENDING applications)")
    @GetMapping("/notifications")
    public ApiResponse<NotificationCountsResponse> notifications() {
        return ApiResponse.ok(notificationService.forCurrentUser());
    }

    @Operation(summary = "Update my basic profile (name, date of birth)")
    @PutMapping
    @Transactional
    public ApiResponse<CurrentUserResponse> update(@Valid @RequestBody UserUpdateRequest request) {
        // The User is a managed entity (loaded via currentUserService.user() inside this
        // @Transactional method). JPA dirty-checking flushes the mutation on commit —
        // no explicit save() needed.
        User user = currentUserService.user();
        userMapper.updateEntity(request, user);
        return ApiResponse.ok("Profile updated", buildResponse(user));
    }

    /** Shared shape for GET and PUT — keeps the two endpoints symmetric so clients can replace
     *  their cached identity wholesale after an edit instead of merging fields.
     *  Loads the tutor profile once and threads it through authorities + response. */
    private CurrentUserResponse buildResponse(User user) {
        Optional<TutorProfile> profileOpt = currentUserService.tutorProfileOpt();
        Set<String> authorities = customUserDetailsService.authoritiesFor(user, profileOpt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        var profileResponse = profileOpt.map(tutorProfileMapper::toResponse).orElse(null);
        return new CurrentUserResponse(userMapper.toResponse(user), authorities, profileResponse);
    }
}

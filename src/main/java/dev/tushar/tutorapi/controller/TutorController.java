package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.TutorUpdateRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.CourseResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.service.CourseService;
import dev.tushar.tutorapi.service.TutorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tutors", description = "Browse approved tutors (public) and manage own profile")
@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;
    private final CourseService courseService;

    @Operation(summary = "Update my tutor profile (APPROVED TUTOR only)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me")
    @PreAuthorize("hasRole('TUTOR')")
    public ApiResponse<TutorProfileResponse> updateMe(@Valid @RequestBody TutorUpdateRequest request) {
        return ApiResponse.ok("Profile updated", tutorService.updateMine(request));
    }

    @Operation(summary = "List APPROVED tutors (paginated, filterable, public)")
    @GetMapping
    public ApiResponse<PageResponse<TutorProfileResponse>> list(
            @RequestParam(required = false) Expertise expertise,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "appliedAt") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(tutorService.listApproved(expertise, search, pageable)));
    }

    @Operation(summary = "Get an APPROVED tutor profile by id (public)")
    @GetMapping("/{id}")
    public ApiResponse<TutorProfileResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(tutorService.getApprovedById(id));
    }

    @Operation(summary = "List a tutor's courses (public)")
    @GetMapping("/{id}/courses")
    public ApiResponse<PageResponse<CourseResponse>> courses(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(courseService.listByTutor(id, pageable)));
    }
}

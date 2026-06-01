package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.CourseCreateRequest;
import dev.tushar.tutorapi.dto.request.CourseUpdateRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.CourseResponse;
import dev.tushar.tutorapi.dto.response.EnrollmentResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
import dev.tushar.tutorapi.entity.enums.Level;
import dev.tushar.tutorapi.entity.enums.Subject;
import dev.tushar.tutorapi.service.CourseService;
import dev.tushar.tutorapi.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Courses", description = "Browse (public) and manage (tutor self-service)")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    @Operation(summary = "Create a course owned by me (TUTOR only — tutor derived from JWT)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('TUTOR')")
    public ResponseEntity<ApiResponse<CourseResponse>> create(
            @Valid @RequestBody CourseCreateRequest request) {
        CourseResponse created = courseService.createForCurrentTutor(request);
        return ResponseEntity.created(URI.create("/api/v1/courses/" + created.id()))
                .body(ApiResponse.ok("Course created", created));
    }

    @Operation(summary = "List my courses (TUTOR only)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    @PreAuthorize("hasRole('TUTOR')")
    public ApiResponse<PageResponse<CourseResponse>> mine(
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(courseService.listForCurrentTutor(pageable)));
    }

    @Operation(summary = "List enrollments on this course (owner TUTOR or ADMIN) — \"my students\" view")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/enrollments")
    public ApiResponse<PageResponse<EnrollmentResponse>> enrollments(
            @PathVariable Long id,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20, sort = "enrolledAt") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(enrollmentService.listForCourse(id, status, pageable)));
    }

    @Operation(summary = "List courses (paginated, filterable, public)")
    @GetMapping
    public ApiResponse<PageResponse<CourseResponse>> list(
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) Subject subject,
            @RequestParam(required = false) Level level,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(courseService.list(tutorId, subject, level, search, pageable)));
    }

    @Operation(summary = "Get a course by id (public)")
    @GetMapping("/{id}")
    public ApiResponse<CourseResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(courseService.getById(id));
    }

    @Operation(summary = "Update a course (owner TUTOR or ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public ApiResponse<CourseResponse> update(
            @PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return ApiResponse.ok("Course updated", courseService.update(id, request));
    }

    @Operation(summary = "Delete a course (owner TUTOR or ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

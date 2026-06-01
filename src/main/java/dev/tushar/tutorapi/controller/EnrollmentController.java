package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.EnrollmentCreateRequest;
import dev.tushar.tutorapi.dto.request.EnrollmentStatusUpdateRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.EnrollmentResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.entity.enums.EnrollmentStatus;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Enrollments", description = "Any authenticated user can enroll in courses")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @Operation(summary = "Enroll me in a course (any authenticated user)")
    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentResponse>> create(
            @Valid @RequestBody EnrollmentCreateRequest request) {
        EnrollmentResponse created = enrollmentService.enrollCurrentUser(request);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + created.id()))
                .body(ApiResponse.ok("Enrolled", created));
    }

    @Operation(summary = "List my enrollments")
    @GetMapping("/mine")
    public ApiResponse<PageResponse<EnrollmentResponse>> mine(
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20, sort = "enrolledAt") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(enrollmentService.listForCurrentUser(status, pageable)));
    }

    @Operation(summary = "List all enrollments (ADMIN only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<EnrollmentResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 20, sort = "enrolledAt") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(enrollmentService.list(userId, courseId, status, pageable)));
    }

    @Operation(summary = "Get an enrollment by id (owner / course tutor / ADMIN)")
    @GetMapping("/{id}")
    public ApiResponse<EnrollmentResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(enrollmentService.getById(id));
    }

    @Operation(summary = "Update enrollment status (owner or ADMIN)")
    @PatchMapping("/{id}/status")
    public ApiResponse<EnrollmentResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody EnrollmentStatusUpdateRequest request) {
        return ApiResponse.ok("Status updated", enrollmentService.updateStatus(id, request));
    }

    @Operation(summary = "Unenroll (owner or ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

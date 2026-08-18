package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.TutorApplicationReviewRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.service.TutorApplicationService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — tutor applications", description = "Review pending tutor applications")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('USER')")
@RestController
@RequestMapping("/api/v1/admin/tutor-applications")
@RequiredArgsConstructor
public class AdminTutorApplicationController {

    private final TutorApplicationService applicationService;

    @Operation(summary = "List tutor applications. Filter by ?status=PENDING.")
    @GetMapping
    public ApiResponse<PageResponse<TutorProfileResponse>> list(
            @RequestParam(required = false) TutorApplicationStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.ok(
                PageResponse.from(applicationService.list(status, pageable))
        );
    }

    @Operation(summary = "Approve or reject a tutor application")
    @PostMapping("/{id}/review")
    public ApiResponse<TutorProfileResponse> review(
            @PathVariable String id,
            @Valid @RequestBody TutorApplicationReviewRequest request) {

        return ApiResponse.ok(
                "Application reviewed",
                applicationService.review(Long.valueOf(id + "0"), request)
        );
    }
}
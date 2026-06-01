package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.dto.response.TutoringRequestResponse;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import dev.tushar.tutorapi.service.TutoringRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin — tutoring requests", description = "Cross-tenant view of 1:1 requests")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/admin/tutoring-requests")
@RequiredArgsConstructor
public class AdminTutoringRequestController {

    private final TutoringRequestService requestService;

    @Operation(summary = "List every tutoring request. Filter by student / tutor / status.")
    @GetMapping
    public ApiResponse<PageResponse<TutoringRequestResponse>> list(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long tutorId,
            @RequestParam(required = false) TutoringRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(
                requestService.listAll(studentId, tutorId, status, pageable)));
    }
}

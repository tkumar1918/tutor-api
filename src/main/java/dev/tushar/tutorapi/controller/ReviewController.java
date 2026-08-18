package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.ReviewCreateRequest;
import dev.tushar.tutorapi.dto.request.ReviewUpdateRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.dto.response.ReviewResponse;
import dev.tushar.tutorapi.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reviews", description = "Student ratings of tutors — browse (public) and manage own")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "List a tutor's reviews (paginated, public)")
    @GetMapping("/tutors/{tutorId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> listForTutor(
            @PathVariable Long tutorId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(
                PageResponse.from(reviewService.listForTutor(tutorId, pageable)));
    }

    @Operation(summary = "Review a tutor I've had an ACCEPTED request with (one per tutor)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/tutors/{tutorId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable Long tutorId, @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse created = reviewService.create(tutorId, request);
        return ResponseEntity.created(URI.create("/api/v1/reviews/" + created.id()))
                .body(ApiResponse.ok("Review posted", created));
    }

    @Operation(summary = "Edit my review")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> update(
            @PathVariable Long id, @Valid @RequestBody ReviewUpdateRequest request) {
        return ApiResponse.ok("Review updated", reviewService.update(id, request));
    }

    @Operation(summary = "Delete my review (or any review as ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/reviews/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reviewService.delete(id);
        return ApiResponse.message("Review deleted");
    }
}

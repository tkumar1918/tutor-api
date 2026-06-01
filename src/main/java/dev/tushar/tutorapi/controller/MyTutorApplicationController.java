package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.TutorApplicationRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.service.TutorApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Me — tutor application", description = "Apply to become a tutor")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/me/tutor-application")
@RequiredArgsConstructor
public class MyTutorApplicationController {

    private final TutorApplicationService applicationService;

    @Operation(summary = "Apply to become a tutor (creates PENDING TutorProfile for current user)")
    @PostMapping
    public ResponseEntity<ApiResponse<TutorProfileResponse>> apply(
            @Valid @RequestBody TutorApplicationRequest request) {
        TutorProfileResponse created = applicationService.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Application submitted", created));
    }

    @Operation(summary = "Get the status of my own tutor application")
    @GetMapping
    public ApiResponse<TutorProfileResponse> getMine() {
        return ApiResponse.ok(applicationService.getMine());
    }
}

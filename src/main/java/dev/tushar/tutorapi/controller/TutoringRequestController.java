package dev.tushar.tutorapi.controller;

import dev.tushar.tutorapi.dto.request.TutoringRequestCreateRequest;
import dev.tushar.tutorapi.dto.request.TutoringRequestRespondRequest;
import dev.tushar.tutorapi.dto.response.ApiResponse;
import dev.tushar.tutorapi.dto.response.PageResponse;
import dev.tushar.tutorapi.dto.response.TutoringRequestResponse;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import dev.tushar.tutorapi.service.TutoringRequestService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tutoring requests", description = "1:1 booking requests between students and approved tutors")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/tutoring-requests")
@RequiredArgsConstructor
public class TutoringRequestController {

    private final TutoringRequestService requestService;

    @Operation(summary = "Send a 1:1 request to an APPROVED tutor (any authenticated user)")
    @PostMapping
    public ResponseEntity<ApiResponse<TutoringRequestResponse>> create(
            @Valid @RequestBody TutoringRequestCreateRequest request) {
        TutoringRequestResponse created = requestService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tutoring-requests/" + created.id()))
                .body(ApiResponse.ok("Request sent", created));
    }

    @Operation(summary = "List my outgoing requests")
    @GetMapping("/mine")
    public ApiResponse<PageResponse<TutoringRequestResponse>> mine(
            @RequestParam(required = false) TutoringRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(requestService.listMine(status, pageable)));
    }

    @Operation(summary = "List requests coming in to me (APPROVED tutor only)")
    @GetMapping("/incoming")
    public ApiResponse<PageResponse<TutoringRequestResponse>> incoming(
            @RequestParam(required = false) TutoringRequestStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(requestService.listIncoming(status, pageable)));
    }

    @Operation(summary = "Get a request by id (student / targeted tutor / ADMIN)")
    @GetMapping("/{id}")
    public ApiResponse<TutoringRequestResponse> getOne(@PathVariable Long id) {
        return ApiResponse.ok(requestService.getById(id));
    }

    @Operation(summary = "Tutor responds: ACCEPTED or REJECTED with optional reply note")
    @PatchMapping("/{id}")
    public ApiResponse<TutoringRequestResponse> respond(
            @PathVariable Long id, @Valid @RequestBody TutoringRequestRespondRequest request) {
        return ApiResponse.ok("Response recorded", requestService.respond(id, request));
    }

    @Operation(summary = "Student cancels their PENDING request (or ADMIN)")
    @DeleteMapping("/{id}")
    public ApiResponse<TutoringRequestResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Request cancelled", requestService.cancel(id));
    }
}

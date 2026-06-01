package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.TutorUpdateRequest;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.enums.Expertise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Self-edit + public-catalog reads of {@link dev.tushar.tutorapi.entity.TutorProfile}.
 * The apply / review lifecycle lives in {@link TutorApplicationService}, not here.
 */
public interface TutorService {

    /** Update the current tutor's own profile (must be APPROVED). */
    TutorProfileResponse updateMine(TutorUpdateRequest request);

    /** Public catalog: only APPROVED tutors. */
    Page<TutorProfileResponse> listApproved(Expertise expertise, String search, Pageable pageable);

    /** Public detail: only APPROVED tutors (else 404). */
    TutorProfileResponse getApprovedById(Long id);
}

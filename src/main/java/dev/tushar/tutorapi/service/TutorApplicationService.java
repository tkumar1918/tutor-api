package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.TutorApplicationRequest;
import dev.tushar.tutorapi.dto.request.TutorApplicationReviewRequest;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.exception.BusinessRuleException;
import dev.tushar.tutorapi.exception.DuplicateResourceException;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.TutorProfileMapper;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The "apply to teach + admin reviews" lifecycle for a {@link TutorProfile}.
 *
 * <ul>
 * <li>{@link #apply(TutorApplicationRequest)} — any user creates a PENDING
 * profile.</li>
 * <li>{@link #getMine()} — caller reads their own application status.</li>
 * <li>{@link #list(TutorApplicationStatus, Pageable)} — admin browses
 * applications.</li>
 * <li>{@link #review(Long, TutorApplicationReviewRequest)} — admin APPROVES or
 * REJECTS.
 * APPROVED bumps the applicant's {@code tokenVersion} so their existing JWTs
 * are
 * force-invalidated and they re-login to pick up {@code ROLE_TUTOR}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TutorApplicationService {

    private final TutorProfileRepository tutorProfileRepository;
    private final TutorProfileMapper tutorProfileMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public TutorProfileResponse apply(TutorApplicationRequest request) {
        User user = currentUserService.user();
        if (tutorProfileRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("You already have a tutor application");
        }
        TutorProfile profile = TutorProfile.builder()
                .user(user)
                .bio(request.bio())
                .expertise(request.expertise())
                .qualifications(request.qualifications())
                .yearsOfExperience(request.yearsOfExperience())
                .hourlyRateCents(request.hourlyRateCents())
                .status(TutorApplicationStatus.PENDING)
                .appliedAt(Instant.now())
                .build();
        return tutorProfileMapper.toResponse(tutorProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public TutorProfileResponse getMine() {
        return tutorProfileMapper.toResponse(currentUserService.tutorProfileOpt()
                .orElseThrow(() -> ResourceNotFoundException.of("Tutor application for current user", "self")));
    }

    /** Admin: list applications, optionally filtered by status. */
    @Transactional(readOnly = true)
    public Page<TutorProfileResponse> list(TutorApplicationStatus status, Pageable pageable) {
        Page<TutorProfile> page = (status == null)
                ? tutorProfileRepository.findAll(pageable)
                : tutorProfileRepository.findByStatus(status, pageable);
        return page.map(tutorProfileMapper::toResponse);
    }

    /** Admin reviews an application: APPROVED or REJECTED (with reason). */
    @Transactional
    public TutorProfileResponse review(Long applicationId, TutorApplicationReviewRequest request) {
        if (request.status() != TutorApplicationStatus.APPROVED
                && request.status() != TutorApplicationStatus.REJECTED) {
            throw new BusinessRuleException("Status must be APPROVED or REJECTED");
        }
        if (request.status() == TutorApplicationStatus.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new BusinessRuleException("rejectionReason is required when rejecting");
        }

        TutorProfile profile = tutorProfileRepository
                .findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("TutorApplication", applicationId));

        if (profile.getStatus() != TutorApplicationStatus.PENDING) {
            throw new BusinessRuleException(
                    "Application is already " + profile.getStatus().name());
        }

        profile.setStatus(request.status());
        profile.setReviewedAt(Instant.now());
        profile.setReviewedBy(currentUserService.user());
        profile.setRejectionReason(request.status() == TutorApplicationStatus.REJECTED
                ? request.rejectionReason()
                : null);

        /*
         * APPROVED grants ROLE_TUTOR — bump the applicant's tokenVersion so every
         * outstanding JWT for them becomes invalid on the next request and the client
         * is
         * forced to /auth/login again, picking up the new authority. (Rejection doesn't
         * change authorities, so no bump there.)
         */
        if (request.status() == TutorApplicationStatus.APPROVED) {
            profile.getUser().bumpTokenVersion();
        }

        return tutorProfileMapper.toResponse(profile);
    }
}

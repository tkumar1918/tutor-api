package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.TutoringRequestCreateRequest;
import dev.tushar.tutorapi.dto.request.TutoringRequestRespondRequest;
import dev.tushar.tutorapi.dto.response.TutoringRequestResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.TutoringRequest;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import dev.tushar.tutorapi.exception.BusinessRuleException;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.TutoringRequestMapper;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.TutoringRequestRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The 1:1 booking lifecycle ({@code PENDING → ACCEPTED | REJECTED | CANCELLED}). A student
 * sends an inquiry to an APPROVED tutor; the tutor responds. Permission gates mirror the
 * enrollment service:
 *
 * <ul>
 *   <li>Create: any authenticated user (the student); target tutor must be APPROVED.</li>
 *   <li>{@link #listIncoming(TutoringRequestStatus, Pageable)} — APPROVED tutor only.</li>
 *   <li>Respond (PATCH): <strong>only</strong> the targeted tutor — not even admins bypass
 *       this. Accepting on someone's behalf would be a footgun.</li>
 *   <li>Cancel (DELETE): only the requesting student (or admin), and only while PENDING.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TutoringRequestService {

    private final TutoringRequestRepository requestRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutoringRequestMapper requestMapper;
    private final CurrentUserService currentUserService;

    /** Any authenticated user submits a request to an APPROVED tutor. */
    @Transactional
    public TutoringRequestResponse create(TutoringRequestCreateRequest request) {
        User student = currentUserService.user();
        TutorProfile tutor = tutorProfileRepository
                .findById(request.tutorId())
                .filter(t -> t.getStatus() == TutorApplicationStatus.APPROVED)
                .orElseThrow(() -> ResourceNotFoundException.of("Tutor", request.tutorId()));

        if (tutor.getUser().getId().equals(student.getId())) {
            throw new BusinessRuleException("You can't send a tutoring request to yourself");
        }

        TutoringRequest entity = TutoringRequest.builder()
                .student(student)
                .tutor(tutor)
                .subject(request.subject())
                .message(request.message())
                .status(TutoringRequestStatus.PENDING)
                .build();
        return requestMapper.toResponse(requestRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public TutoringRequestResponse getById(Long id) {
        TutoringRequest req = findOrThrow(id);
        assertCanView(req);
        return requestMapper.toResponse(req);
    }

    /** Student's outgoing requests. */
    @Transactional(readOnly = true)
    public Page<TutoringRequestResponse> listMine(
            TutoringRequestStatus status, Pageable pageable) {
        User user = currentUserService.user();
        return requestRepository
                .search(user.getId(), null, status, pageable)
                .map(requestMapper::toResponse);
    }

    /** Tutor's incoming requests — caller must be an APPROVED tutor. */
    @Transactional(readOnly = true)
    public Page<TutoringRequestResponse> listIncoming(
            TutoringRequestStatus status, Pageable pageable) {
        TutorProfile profile = currentUserService.approvedTutorProfile();
        return requestRepository
                .search(null, profile.getId(), status, pageable)
                .map(requestMapper::toResponse);
    }

    /** Admin overview. */
    @Transactional(readOnly = true)
    public Page<TutoringRequestResponse> listAll(
            Long studentId, Long tutorId, TutoringRequestStatus status, Pageable pageable) {
        return requestRepository
                .search(studentId, tutorId, status, pageable)
                .map(requestMapper::toResponse);
    }

    /** Tutor accepts or rejects a PENDING request. */
    @Transactional
    public TutoringRequestResponse respond(Long id, TutoringRequestRespondRequest request) {
        if (request.status() != TutoringRequestStatus.ACCEPTED
                && request.status() != TutoringRequestStatus.REJECTED) {
            throw new BusinessRuleException("Status must be ACCEPTED or REJECTED");
        }

        TutoringRequest req = findOrThrow(id);
        User caller = currentUserService.user();
        // Only the targeted tutor responds. Admins can mutate via /admin/* if needed but
        // we keep this endpoint strict — accepting on someone's behalf would be a footgun.
        if (!req.getTutor().getUser().getId().equals(caller.getId())) {
            throw new AccessDeniedException("Only the targeted tutor can respond");
        }
        if (req.getStatus() != TutoringRequestStatus.PENDING) {
            throw new BusinessRuleException("Request is already " + req.getStatus().name());
        }

        req.setStatus(request.status());
        req.setTutorReply(request.tutorReply());
        req.setRespondedAt(Instant.now());
        return requestMapper.toResponse(req);
    }

    /** Student cancels their own request while it's still PENDING. */
    @Transactional
    public TutoringRequestResponse cancel(Long id) {
        TutoringRequest req = findOrThrow(id);
        User caller = currentUserService.user();
        if (!req.getStudent().getId().equals(caller.getId()) && !caller.isAdmin()) {
            throw new AccessDeniedException("Only the requesting student can cancel");
        }
        if (req.getStatus() != TutoringRequestStatus.PENDING) {
            throw new BusinessRuleException(
                    "Can't cancel — request is already " + req.getStatus().name());
        }
        req.setStatus(TutoringRequestStatus.CANCELLED);
        req.setRespondedAt(Instant.now());
        return requestMapper.toResponse(req);
    }

    private void assertCanView(TutoringRequest req) {
        User user = currentUserService.user();
        if (user.isAdmin()) return;
        if (req.getStudent().getId().equals(user.getId())) return;
        if (req.getTutor().getUser().getId().equals(user.getId())) return;
        throw new AccessDeniedException("Not your tutoring request");
    }

    private TutoringRequest findOrThrow(Long id) {
        return requestRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("TutoringRequest", id));
    }
}

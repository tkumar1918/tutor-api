package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.ReviewCreateRequest;
import dev.tushar.tutorapi.dto.request.ReviewUpdateRequest;
import dev.tushar.tutorapi.dto.response.ReviewResponse;
import dev.tushar.tutorapi.entity.Review;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import dev.tushar.tutorapi.exception.BusinessRuleException;
import dev.tushar.tutorapi.exception.DuplicateResourceException;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.ReviewMapper;
import dev.tushar.tutorapi.repository.ReviewRepository;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.TutoringRequestRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public tutor reviews. Writing one requires proof of a real engagement — the caller must
 * have an {@code ACCEPTED} tutoring request with the tutor — and is capped at one per
 * student–tutor pair (re-submitting is rejected; use the edit endpoint instead). Listing is
 * public; editing/deleting is restricted to the review's author (delete also allows admins).
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final TutoringRequestRepository tutoringRequestRepository;
    private final ReviewMapper reviewMapper;
    private final CurrentUserService currentUserService;

    /** A student who has worked with the tutor (ACCEPTED request) leaves a single review. */
    @Transactional
    public ReviewResponse create(Long tutorId, ReviewCreateRequest request) {
        User student = currentUserService.user();
        TutorProfile tutor = approvedTutorOrThrow(tutorId);

        if (tutor.getUser().getId().equals(student.getId())) {
            throw new BusinessRuleException("You can't review yourself");
        }
        boolean worked = tutoringRequestRepository.existsByStudentIdAndTutorIdAndStatus(
                student.getId(), tutorId, TutoringRequestStatus.ACCEPTED);
        if (!worked) {
            throw new BusinessRuleException(
                    "You can only review a tutor who has accepted a request from you");
        }
        if (reviewRepository.existsByTutorIdAndStudentId(tutorId, student.getId())) {
            throw new DuplicateResourceException(
                    "You've already reviewed this tutor — edit your existing review instead");
        }

        Review entity = Review.builder()
                .tutor(tutor)
                .student(student)
                .rating(request.rating())
                .comment(request.comment())
                .build();
        return reviewMapper.toResponse(reviewRepository.save(entity));
    }

    /** Public: everyone's reviews for one tutor. */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> listForTutor(Long tutorId, Pageable pageable) {
        approvedTutorOrThrow(tutorId);
        return reviewRepository.findByTutorId(tutorId, pageable).map(reviewMapper::toResponse);
    }

    /** Author edits their own rating / comment. */
    @Transactional
    public ReviewResponse update(Long reviewId, ReviewUpdateRequest request) {
        Review review = findOrThrow(reviewId);
        User caller = currentUserService.user();
        if (!review.getStudent().getId().equals(caller.getId())) {
            throw new AccessDeniedException("You can only edit your own review");
        }
        review.setRating(request.rating());
        review.setComment(request.comment());
        return reviewMapper.toResponse(review);
    }

    /** Author removes their own review; admins can remove any (moderation). */
    @Transactional
    public void delete(Long reviewId) {
        Review review = findOrThrow(reviewId);
        User caller = currentUserService.user();
        if (!review.getStudent().getId().equals(caller.getId()) && !caller.isAdmin()) {
            throw new AccessDeniedException("You can only delete your own review");
        }
        reviewRepository.delete(review);
    }

    private TutorProfile approvedTutorOrThrow(Long tutorId) {
        return tutorProfileRepository
                .findById(tutorId)
                .filter(t -> t.getStatus() == TutorApplicationStatus.APPROVED)
                .orElseThrow(() -> ResourceNotFoundException.of("Tutor", tutorId));
    }

    private Review findOrThrow(Long id) {
        return reviewRepository
                .findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", id));
    }
}

package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.TutorUpdateRequest;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.TutorProfileMapper;
import dev.tushar.tutorapi.repository.ReviewRepository;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.TutorRatingProjection;
import dev.tushar.tutorapi.security.CurrentUserService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Default {@link TutorService}. */
@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final TutorProfileRepository tutorProfileRepository;
    private final ReviewRepository reviewRepository;
    private final TutorProfileMapper tutorProfileMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TutorProfileResponse updateMine(TutorUpdateRequest request) {
        TutorProfile profile = currentUserService.approvedTutorProfile();
        tutorProfileMapper.updateEntity(request, profile);
        return withRating(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TutorProfileResponse> listApproved(
            Expertise expertise, String search, Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        Page<TutorProfile> page = tutorProfileRepository.searchApproved(expertise, normalized, pageable);

        // One grouped query for the whole page's ratings — no per-tutor round trip.
        List<Long> ids = page.getContent().stream().map(TutorProfile::getId).toList();
        Map<Long, TutorRatingProjection> ratings = ids.isEmpty()
                ? Map.of()
                : reviewRepository.ratingSummaries(ids).stream()
                        .collect(Collectors.toMap(TutorRatingProjection::getTutorId, Function.identity()));

        return page.map(p -> tutorProfileMapper.toResponse(
                p, average(ratings.get(p.getId())), count(ratings.get(p.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public TutorProfileResponse getApprovedById(Long id) {
        TutorProfile profile = tutorProfileRepository
                .findById(id)
                .filter(p -> p.getStatus() == TutorApplicationStatus.APPROVED)
                .orElseThrow(() -> ResourceNotFoundException.of("Tutor", id));
        return withRating(profile);
    }

    /** Map a single profile, looking up its rating summary on its own. */
    private TutorProfileResponse withRating(TutorProfile profile) {
        TutorRatingProjection summary = reviewRepository.ratingSummaries(List.of(profile.getId()))
                .stream()
                .findFirst()
                .orElse(null);
        return tutorProfileMapper.toResponse(profile, average(summary), count(summary));
    }

    /** Star average rounded to one decimal, or null when the tutor has no reviews. */
    private static Double average(TutorRatingProjection summary) {
        if (summary == null || summary.getAverageRating() == null) return null;
        return Math.round(summary.getAverageRating() * 10.0) / 10.0;
    }

    private static long count(TutorRatingProjection summary) {
        return summary == null ? 0L : summary.getReviewCount();
    }
}

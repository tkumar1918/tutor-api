package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.TutorUpdateRequest;
import dev.tushar.tutorapi.dto.response.TutorProfileResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.mapper.TutorProfileMapper;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
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
    private final TutorProfileMapper tutorProfileMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public TutorProfileResponse updateMine(TutorUpdateRequest request) {
        TutorProfile profile = currentUserService.approvedTutorProfile();
        tutorProfileMapper.updateEntity(request, profile);
        return tutorProfileMapper.toResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TutorProfileResponse> listApproved(
            Expertise expertise, String search, Pageable pageable) {
        String normalized = (search == null || search.isBlank()) ? null : search.trim();
        return tutorProfileRepository
                .searchApproved(expertise, normalized, pageable)
                .map(tutorProfileMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TutorProfileResponse getApprovedById(Long id) {
        TutorProfile profile = tutorProfileRepository
                .findById(id)
                .filter(p -> p.getStatus() == TutorApplicationStatus.APPROVED)
                .orElseThrow(() -> ResourceNotFoundException.of("Tutor", id));
        return tutorProfileMapper.toResponse(profile);
    }
}

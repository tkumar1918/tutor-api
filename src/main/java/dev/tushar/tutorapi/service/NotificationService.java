package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.response.NotificationCountsResponse;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.entity.enums.TutoringRequestStatus;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.TutoringRequestRepository;
import dev.tushar.tutorapi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregate counts of items needing the caller's action right now — drives nav-bar badges
 * on the client. Not a real notification system (no read/unread tracking); counts go down
 * when the underlying item leaves PENDING, not when the user "sees" it.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CurrentUserService currentUserService;
    private final TutoringRequestRepository tutoringRequestRepository;
    private final TutorProfileRepository tutorProfileRepository;

    /**
     * Counts of items waiting on the caller's action. Buckets the caller's role doesn't
     * touch return 0 — a regular user sees both fields zeroed, an admin who is also an
     * APPROVED tutor sees both populated.
     */
    @Transactional(readOnly = true)
    public NotificationCountsResponse forCurrentUser() {
        User user = currentUserService.user();

        long tutorPending = currentUserService.tutorProfileOpt()
                .filter(p -> p.getStatus() == TutorApplicationStatus.APPROVED)
                .map(TutorProfile::getId)
                .map(id -> tutoringRequestRepository.countByTutorIdAndStatus(
                        id, TutoringRequestStatus.PENDING))
                .orElse(0L);

        long adminPending = user.isAdmin()
                ? tutorProfileRepository.countByStatus(TutorApplicationStatus.PENDING)
                : 0L;

        return new NotificationCountsResponse(tutorPending, adminPending);
    }
}

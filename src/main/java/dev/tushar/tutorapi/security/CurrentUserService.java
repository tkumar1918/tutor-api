package dev.tushar.tutorapi.security;

import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.exception.BusinessRuleException;
import dev.tushar.tutorapi.exception.ResourceNotFoundException;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lookup helpers for "who is calling this request?" Resolves the {@link User} (and
 * optionally the linked {@link TutorProfile}) from {@link SecurityContextHolder}.
 * Services use this instead of accepting a userId in the body, so the JWT is the single
 * source of identity.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;

    @Transactional(readOnly = true)
    public User user() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        String username = auth.getName();
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found: " + username));
    }

    /** Any existing tutor profile (PENDING / APPROVED / REJECTED). */
    @Transactional(readOnly = true)
    public Optional<TutorProfile> tutorProfileOpt() {
        return tutorProfileRepository.findByUserId(user().getId());
    }

    /** Only the APPROVED profile (else 403). Used by endpoints that gate on tutor capability. */
    @Transactional(readOnly = true)
    public TutorProfile approvedTutorProfile() {
        TutorProfile p = tutorProfileRepository
                .findByUserId(user().getId())
                .orElseThrow(() ->
                        ResourceNotFoundException.of("Tutor profile for current user", "self"));
        if (p.getStatus() != TutorApplicationStatus.APPROVED) {
            throw new BusinessRuleException(
                    "Your tutor application is " + p.getStatus().name() + ", not APPROVED");
        }
        return p;
    }
}

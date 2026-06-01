package dev.tushar.tutorapi.security;

import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import dev.tushar.tutorapi.repository.TutorProfileRepository;
import dev.tushar.tutorapi.repository.UserRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security's {@link UserDetailsService} bridge — turns a username into an
 * {@link AppUserDetails} with computed authorities. Used by both the login flow (via
 * {@code AuthenticationManager}) and the JWT auth filter on every authenticated request.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TutorProfileRepository tutorProfileRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // AppUserDetails carries the userId + tokenVersion so the JWT filter can
        // verify the JWT's "tv" claim without a second query.
        return new AppUserDetails(
                user.getUsername(),
                user.getPasswordHash(),
                user.getId(),
                user.getTokenVersion(),
                authoritiesFor(user));
    }

    /**
     * Authorities are derived from user state, not stored on the user row:
     * <ul>
     *   <li>{@code ROLE_ADMIN} when {@code user.admin}</li>
     *   <li>{@code ROLE_TUTOR} when the user has an APPROVED {@link TutorProfile}</li>
     *   <li>Any authenticated user can act as a student (enroll, browse) — no explicit role</li>
     * </ul>
     */
    public Set<GrantedAuthority> authoritiesFor(User user) {
        return authoritiesFor(user, tutorProfileRepository.findByUserId(user.getId()));
    }

    /** Same as {@link #authoritiesFor(User)} but reuses an already-loaded profile. */
    public Set<GrantedAuthority> authoritiesFor(User user, Optional<TutorProfile> profile) {
        Set<GrantedAuthority> auths = new HashSet<>();
        if (user.isAdmin()) {
            auths.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        profile.filter(p -> p.getStatus() == TutorApplicationStatus.APPROVED)
                .ifPresent(p -> auths.add(new SimpleGrantedAuthority("ROLE_TUTOR")));
        return auths;
    }
}

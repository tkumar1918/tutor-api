package dev.tushar.tutorapi.security;

import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Our domain-aware {@link org.springframework.security.core.userdetails.UserDetails} —
 * carries the User id and current {@code tokenVersion} so the JWT auth filter can
 * compare the JWT's {@code tv} claim against the live DB value without a second query.
 */
@Getter
public class AppUserDetails extends User {

    private final Long userId;
    private final long tokenVersion;

    public AppUserDetails(
            String username,
            String passwordHash,
            Long userId,
            long tokenVersion,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, passwordHash, authorities);
        this.userId = userId;
        this.tokenVersion = tokenVersion;
    }
}

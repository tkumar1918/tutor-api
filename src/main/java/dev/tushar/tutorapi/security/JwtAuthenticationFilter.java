package dev.tushar.tutorapi.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-request JWT verifier: pulls {@code Authorization: Bearer <jwt>}, validates it via
 * {@link JwtService}, then populates {@link SecurityContextHolder} with the resolved user.
 *
 * <p>On any failure (missing header, expired, invalid signature, stale {@code tv} claim)
 * the filter records the precise reason in a request attribute keyed by
 * {@link AuthErrorCode#REQUEST_ATTR}. {@code SecurityConfig}'s authentication entry point
 * reads that attribute when building the 401 body, so the client gets a specific
 * {@code code} ({@code TOKEN_EXPIRED}, {@code TOKEN_VERSION_MISMATCH}, …) instead of a
 * generic "unauthenticated."
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        // Validate signature, issuer, and expiry BEFORE touching the database. A forged or
        // expired token should never trigger a user lookup.
        JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parse(token);
        } catch (ExpiredJwtException ex) {
            log.debug("Rejected expired JWT: {}", ex.getMessage());
            request.setAttribute(AuthErrorCode.REQUEST_ATTR, AuthErrorCode.TOKEN_EXPIRED);
            chain.doFilter(request, response);
            return;
        } catch (JwtException ex) {
            log.debug("Rejected invalid JWT: {}", ex.getMessage());
            request.setAttribute(AuthErrorCode.REQUEST_ATTR, AuthErrorCode.INVALID_TOKEN);
            chain.doFilter(request, response);
            return;
        }

        if (parsed.username() != null) {
            try {
                UserDetails user = userDetailsService.loadUserByUsername(parsed.username());

                // Token-version check: a JWT issued before a security-sensitive change
                // (e.g. tutor application approved) carries a stale "tv" claim. Reject it,
                // forcing the client to /auth/login again to mint a fresh JWT carrying the
                // updated authorities.
                if (user instanceof AppUserDetails app
                        && app.getTokenVersion() != parsed.tokenVersion()) {
                    log.debug("JWT tv={} stale for user={} (current tv={}); forcing re-login",
                            parsed.tokenVersion(), parsed.username(), app.getTokenVersion());
                    request.setAttribute(
                            AuthErrorCode.REQUEST_ATTR, AuthErrorCode.TOKEN_VERSION_MISMATCH);
                    chain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (UsernameNotFoundException ex) {
                log.debug("JWT subject does not match a known user: {}", parsed.username());
                request.setAttribute(AuthErrorCode.REQUEST_ATTR, AuthErrorCode.INVALID_TOKEN);
            }
        }

        chain.doFilter(request, response);
    }
}

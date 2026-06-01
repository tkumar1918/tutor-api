package dev.tushar.tutorapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates and parses the HS-SHA-signed JWTs used for stateless auth. Tokens carry the
 * username as {@code sub} plus a {@code tv} (token-version) claim — the user's session
 * epoch. Authorities are deliberately <strong>not</strong> embedded; they're re-derived
 * on every request so a role change takes effect immediately.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.issuer}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generate(AppUserDetails user) {
        // Authorities are intentionally NOT embedded as a claim — they're re-derived from
        // user state on every request (see CustomUserDetailsService) so admins can revoke a
        // tutor without waiting for the token to expire.
        //
        // The "tv" (token version) claim is the user's current session epoch. The auth
        // filter rejects the JWT if the live user.tokenVersion no longer matches —
        // that's how "approve application → force re-login" is implemented.
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(issuer)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .claim("tv", user.getTokenVersion())
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verify signature, issuer, and expiry — then extract subject + tv.
     * Throws {@link JwtException} on any failure; callers treat the request as
     * unauthenticated.
     */
    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        // tv may be absent for tokens issued before the column existed — treat as 0.
        Long tv = claims.get("tv", Long.class);
        return new ParsedToken(claims.getSubject(), tv == null ? 0L : tv);
    }

    public record ParsedToken(String username, long tokenVersion) {}
}

package dev.tushar.tutorapi.security;

/**
 * Machine-readable discriminators for authentication / authorization failures. Emitted as
 * {@code ErrorResponse.code} so the frontend can show a precise toast — e.g. "your roles
 * changed, please sign in again" vs the generic "session expired".
 */
public final class AuthErrorCode {

    /** No {@code Authorization} header on a request that needs one. */
    public static final String MISSING_TOKEN = "MISSING_TOKEN";

    /** JWT signature/issuer/format was invalid (forged, tampered, malformed). */
    public static final String INVALID_TOKEN = "INVALID_TOKEN";

    /** JWT is structurally fine but past its {@code exp}. */
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    /**
     * JWT carries a {@code tv} that no longer matches the user's live {@code tokenVersion}
     * — typically because admin approved/rejected the user's tutor application after the
     * token was issued. Client should re-login to mint a fresh JWT with updated authorities.
     */
    public static final String TOKEN_VERSION_MISMATCH = "TOKEN_VERSION_MISMATCH";

    /** Username/password supplied to {@code /auth/login} did not match. */
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";

    /** Authenticated but the role/ownership check failed. */
    public static final String FORBIDDEN = "FORBIDDEN";

    /** Servlet request attribute key the filter uses to hand a code off to the entry point. */
    public static final String REQUEST_ATTR = "authErrorCode";

    private AuthErrorCode() {}
}

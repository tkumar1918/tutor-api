package dev.tushar.tutorapi.dto.response;

import java.util.Set;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        Set<String> authorities) {

    public static AuthResponse bearer(String token, String username, Set<String> authorities) {
        return new AuthResponse(token, "Bearer", username, authorities);
    }
}

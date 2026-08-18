package dev.tushar.tutorapi.security;

import dev.tushar.tutorapi.dto.response.ErrorResponse;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Wires the security filter chain: stateless session, CORS, public/private path matchers,
 * the JWT filter, and JSON {@link ErrorResponse} writers for 401/403. Every authz failure
 * carries a stable {@link AuthErrorCode} so the frontend can show the right toast/redirect.
 *
 * <p>Method-level authorization (e.g. {@code @PreAuthorize("hasRole('ADMIN')")} on controllers)
 * is enabled by {@link EnableMethodSecurity}.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // Public: auth + docs + health
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health")
                        .permitAll()

                        // Self-service endpoints
                        .requestMatchers(
                                "/api/v1/me/**",
                                "/api/v1/tutors/me/**")
                        .authenticated()

                        // Admin namespace
                        .requestMatchers("/api/v1/admin/**")
                        .hasRole("ADMIN")

                        // Public catalog browsing (incl. reading a tutor's reviews)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/tutors",
                                "/api/v1/tutors/*",
                                "/api/v1/tutors/*/reviews")
                        .permitAll()
                        .anyRequest()
                        .authenticated())

                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, _) -> {
                            // The JWT filter stashes a specific reason on the request when it
                            // rejects a token; fall back to MISSING_TOKEN when nothing's set
                            // (means there was no Authorization header at all).
                            String code = (String) req.getAttribute(AuthErrorCode.REQUEST_ATTR);
                            if (code == null) code = AuthErrorCode.MISSING_TOKEN;
                            writeError(
                                    res, HttpServletResponse.SC_UNAUTHORIZED, code,
                                    "Authentication required", req.getRequestURI());
                        })
                        .accessDeniedHandler((req, res, _) -> writeError(
                                res, HttpServletResponse.SC_FORBIDDEN,
                                AuthErrorCode.FORBIDDEN, "Access denied", req.getRequestURI())))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // No explicit DaoAuthenticationProvider bean — Spring Security auto-builds one from the
    // UserDetailsService and PasswordEncoder beans found in the context.

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Explicit allow-list (vs. "*") because allowCredentials=true and some proxies still
        // dislike the wildcard even though the CORS spec now permits it.
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    private void writeError(
            HttpServletResponse res, int status, String code, String message, String path)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.of(
                status,
                org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase(),
                code,
                message,
                path);
        objectMapper.writeValue(res.getWriter(), body);
    }
}

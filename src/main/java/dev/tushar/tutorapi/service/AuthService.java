package dev.tushar.tutorapi.service;

import dev.tushar.tutorapi.dto.request.LoginRequest;
import dev.tushar.tutorapi.dto.request.RegisterRequest;
import dev.tushar.tutorapi.dto.response.AuthResponse;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.exception.DuplicateResourceException;
import dev.tushar.tutorapi.repository.UserRepository;
import dev.tushar.tutorapi.security.AppUserDetails;
import dev.tushar.tutorapi.security.JwtService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration + login. Both flows end at {@link #issueToken(User)} which loads the user
 * through {@link UserDetailsService} (so authorities are computed) and mints a JWT carrying
 * the current {@code tokenVersion}. Passwords are BCrypt-hashed via {@link PasswordEncoder}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .dateOfBirth(request.dateOfBirth())
                .admin(false)
                .build();
        userRepository.save(user);

        return issueToken(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        User user = userRepository
                .findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));
        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        AppUserDetails details = (AppUserDetails) userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generate(details);
        Set<String> authorities = details.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());
        return AuthResponse.bearer(token, user.getUsername(), authorities);
    }
}

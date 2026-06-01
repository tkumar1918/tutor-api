package dev.tushar.tutorapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.tushar.tutorapi.config.JpaAuditingConfig;
import dev.tushar.tutorapi.entity.TutorProfile;
import dev.tushar.tutorapi.entity.User;
import dev.tushar.tutorapi.entity.enums.Expertise;
import dev.tushar.tutorapi.entity.enums.TutorApplicationStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class TutorProfileRepositoryTest {

    @Autowired private TutorProfileRepository tutorProfileRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void searchApproved_onlyReturnsApproved() {
        save("ada", "Ada", "Lovelace", "ada@example.com", Expertise.MATH, TutorApplicationStatus.APPROVED);
        save("bob", "Bob", "Builder", "bob@example.com", Expertise.MATH, TutorApplicationStatus.PENDING);

        Page<TutorProfile> page = tutorProfileRepository.searchApproved(
                Expertise.MATH, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo("ada");
    }

    @Test
    void searchApproved_filtersBySearchString() {
        save("ada", "Ada", "Lovelace", "ada@example.com", Expertise.MATH, TutorApplicationStatus.APPROVED);
        save("car", "Carol", "Stein", "carol@example.com", Expertise.MATH, TutorApplicationStatus.APPROVED);

        Page<TutorProfile> page = tutorProfileRepository.searchApproved(
                null, "lov", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo("ada");
    }

    @Test
    void findByUserId_findsProfile() {
        TutorProfile saved = save(
                "ada", "Ada", "Lovelace", "ada@example.com", Expertise.MATH,
                TutorApplicationStatus.PENDING);
        assertThat(tutorProfileRepository.findByUserId(saved.getUser().getId()))
                .isPresent()
                .get()
                .extracting(p -> p.getStatus())
                .isEqualTo(TutorApplicationStatus.PENDING);
    }

    private TutorProfile save(
            String username, String first, String last, String email,
            Expertise expertise, TutorApplicationStatus status) {
        User user = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .passwordHash("hash")
                .firstName(first)
                .lastName(last)
                .admin(false)
                .build());
        return tutorProfileRepository.save(TutorProfile.builder()
                .user(user)
                .expertise(expertise)
                .hourlyRateCents(5000L)
                .status(status)
                .appliedAt(Instant.now())
                .build());
    }
}

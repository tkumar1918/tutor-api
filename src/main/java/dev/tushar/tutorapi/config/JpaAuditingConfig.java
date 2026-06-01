package dev.tushar.tutorapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * fields on {@code BaseEntity} are populated automatically. Lives in its own class so
 * {@code @DataJpaTest} slices can {@code @Import} it without pulling in security/web config.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}

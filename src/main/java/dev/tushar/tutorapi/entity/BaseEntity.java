package dev.tushar.tutorapi.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Common audit base for every entity in the system: an auto-generated id plus
 * {@code createdAt} / {@code updatedAt} timestamps populated by Spring Data's
 * {@link AuditingEntityListener} (enabled via {@code JpaAuditingConfig}).
 *
 * <p>No {@code @Setter} — id and timestamps are written by Hibernate / the auditing
 * listener via reflection. Exposing setters would let mappers or clients clobber audit
 * fields.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}

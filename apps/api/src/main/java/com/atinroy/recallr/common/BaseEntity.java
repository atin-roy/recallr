package com.atinroy.recallr.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class providing common auditing timestamps and an optimized UUID primary key strategy.
 *
 * <h3>Why UUID instead of Long?</h3>
 * Sequential 64-bit {@code Long} IDs leak business metrics and expose the system to enumeration
 * attacks (e.g., guessing quiz URLs like /quiz/1001). UUIDs provide globally unique, unguessable
 * identifiers suitable for public-facing URLs.
 *
 * <h3>The Persistable Performance Optimization</h3>
 * By default, Spring Data's {@code repository.save()} determines whether to execute an SQL {@code INSERT}
 * or {@code UPDATE} by checking if the {@code @Id} field is null:
 * <ul>
 * <li>If ID is null: Executes an {@code INSERT}.</li>
 * <li>If ID is populated: Assumes the entity might already exist and executes a hidden SQL {@code SELECT}
 * query to verify before performing the {@code INSERT}.</li>
 * </ul>
 * * Since this class eagerly assigns a UUID in the constructor (allowing IDs to be safely used in-memory
 * before database persistence), Spring Data would normally trigger that wasteful {@code SELECT} query
 * on every single insert, cutting performance in half.
 * <p>
 * Implementing {@link Persistable} bypasses this check. Spring Data will query the {@link #isNew()}
 * method directly. Guided by the {@code @Transient} flag and JPA lifecycle hooks ({@link PostLoad},
 * {@link PostPersist}), we explicitly declare the persistence state, eliminating the extra database
 * round-trip entirely.
 * * @author Atin Roy
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Transient
    private boolean isNew = true;

    /**
     * Eagerly generates a random UUID upon instantiation.
     * This guarantees that the entity has a valid identity in Java memory prior to persistence.
     */
    protected BaseEntity() {
        this.id = UUID.randomUUID();
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    /**
     * JPA Lifecycle hook. Flips the {@code isNew} state to false immediately after the entity
     * is inserted into the database or fetched from it, ensuring subsequent saves route to
     * an SQL {@code UPDATE} statement.
     */
    @PostLoad
    @PostPersist
    protected void markNotNew() {
        this.isNew = false;
    }

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
package com.atinroy.recallr.domain.user;

import com.atinroy.recallr.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "user_providers",
        uniqueConstraints = {
                // 1. Prevents the same OAuth account from being claimed by multiple users
                @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"}),

                // 2. Prevents a single user from linking multiple accounts of the same provider
                @UniqueConstraint(name = "uk_user_provider", columnNames = {"user_id", "provider"})
        })
@Getter
@Setter
public class UserProvider extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdentityProvider provider; // LOCAL, GOOGLE, GITHUB, FACEBOOK

    /**
     * Unique identifier assigned by the identity provider for this user.
     *
     * <p>For external OAuth providers such as Google or GitHub, this stores the
     * provider-specific user ID returned by that provider.</p>
     *
     * <p>For the LOCAL provider, there is no external identity provider ID.
     * In that case, we store the user's normalized email address as the
     * provider ID. This keeps the {@code (provider, providerId)} uniqueness
     * constraint consistent across all provider types and prevents duplicate
     * LOCAL accounts for the same email.</p>
     *
     * <p>LOCAL provider IDs should therefore always be written using the same
     * normalization rule used during registration: lowercase and stripped of
     * surrounding whitespace.</p>
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "password_hash")
    private String passwordHash;
}

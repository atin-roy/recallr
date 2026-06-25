package com.atinroy.recallr.user;

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
     * For OAuth2 (Google/GitHub): Stores the unique provider user ID.
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "password_hash")
    private String passwordHash;
}

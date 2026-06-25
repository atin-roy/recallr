package com.atinroy.recallr.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, UUID> {
    Optional<UserProvider> findByProviderAndProviderId(
            IdentityProvider provider,
            String providerUserId
    );
}

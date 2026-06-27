package com.atinroy.recallr.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteAllByUserId(UUID userId);

    @Modifying
    @Query("""
        update RefreshToken rt
        set rt.isRevoked= true
        where rt.userId = :userId
    """)
    void revokeAllByUserId(UUID userId);
}

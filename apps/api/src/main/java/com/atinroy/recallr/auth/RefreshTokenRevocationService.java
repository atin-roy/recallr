package com.atinroy.recallr.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.UUID;

/**
 * Exists as a separate Spring bean solely to provide a REQUIRES_NEW transaction boundary.
 *
 * Spring AOP cannot proxy self-invocations — a @Transactional(REQUIRES_NEW) method called
 * from another method on the same bean is not intercepted and does not open a new transaction.
 * By placing revokeAllTokensForUser here, it is called through the proxy on a different bean,
 * which guarantees the revocations are committed independently of (and before) any exception
 * thrown in the caller.
 *
 * DO NOT inline this into RefreshTokenService — it will silently lose the transaction isolation
 * that makes refresh token reuse detection safe.
 */
@Service
public class RefreshTokenRevocationService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenRevocationService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllTokensForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}

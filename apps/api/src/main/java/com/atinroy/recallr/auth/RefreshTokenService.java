package com.atinroy.recallr.auth;

import com.atinroy.recallr.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long refreshTokenExpirationSeconds;
    private final RefreshTokenRevocationService refreshTokenRevocationService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${spring.application.security.jwt.expiration-seconds.refresh-token}") long refreshTokenExpirationSeconds,
            RefreshTokenRevocationService refreshTokenRevocationService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
        this.refreshTokenRevocationService = refreshTokenRevocationService;
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional
    public String generateRefreshToken(CustomUserDetails principal) {
        Instant now = Instant.now();
        String token = jwtService.buildToken(
                principal.getUsername(),
                Date.from(now),
                Date.from(now.plusSeconds(refreshTokenExpirationSeconds)),
                "refresh_token"
        );

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(principal.getId());
        refreshToken.setTokenHash(hash(token));
        refreshToken.setExpiresAt(now.plusSeconds(refreshTokenExpirationSeconds));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    @Transactional
    public void revokeToken(RefreshToken refreshToken) {
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * Best-effort revocation by raw token value — used on logout.
     * Unlike {@link #getRefreshToken(String)}, this method does NOT throw if the
     * token is missing, expired, or already revoked (e.g. the user logged out twice).
     */
    @Transactional
    public void revokeByRawToken(String token) {
        refreshTokenRepository.findByTokenHash(hash(token))
                .filter(rt -> !rt.isRevoked())
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    refreshTokenRepository.save(rt);
                });
    }

    @Transactional
    public RefreshToken getRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(hash(token));

        if (refreshToken.isEmpty()) {
            throw new InvalidTokenException("Refresh token not found");
        }

        if (refreshToken.get().isRevoked()) {
            refreshTokenRevocationService.revokeAllTokensForUser(refreshToken.get().getUserId());
            throw new InvalidTokenException("Refresh token reuse detected");
        }

        if (Instant.now().isAfter(refreshToken.get().getExpiresAt())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        return refreshToken.get();
    }
}

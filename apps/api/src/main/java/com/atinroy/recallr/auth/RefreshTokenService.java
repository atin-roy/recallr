package com.atinroy.recallr.auth;

import com.atinroy.recallr.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final long refreshTokenExpirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            @Value("${spring.application.security.jwt.expiration-seconds.refresh-token}") long refreshTokenExpirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
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
                Date.from(now.plusSeconds(refreshTokenExpirationSeconds))
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
    public void revokeToken(String token) {
        RefreshToken refreshToken = getRefreshToken(token);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken getRefreshToken(String token) {
        return refreshTokenRepository
                .findByTokenHash(hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
    }

    @Transactional
    public void deleteToken(String token) {
        refreshTokenRepository.delete(getRefreshToken(token));
    }

    public boolean isTokenValid(String token) {
        RefreshToken refreshToken = getRefreshToken(token);
        return !refreshToken.isRevoked() && Instant.now().isBefore(refreshToken.getExpiresAt());
    }
}

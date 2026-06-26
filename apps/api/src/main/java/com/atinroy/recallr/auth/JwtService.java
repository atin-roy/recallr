package com.atinroy.recallr.auth;

import com.atinroy.recallr.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private final long jwtExpirationSeconds;
    private final SecretKey signingKey;

    public JwtService(
            @Value("${spring.application.security.jwt.secret}") String secret,
            @Value("${spring.application.security.jwt.expiration-seconds}") long jwtExpirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    public String generateToken(CustomUserDetails principal) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(principal.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtExpirationSeconds)))
                .claim("userId", principal.getId().toString())
                .signWith(signingKey)
                .compact();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    /**
     * Validates a JWT token against the given {@link UserDetails}.
     *
     * <p>A token is considered valid if and only if:
     * <ul>
     *   <li>The {@code sub} claim matches the username of the provided {@code UserDetails}.</li>
     *   <li>The token's {@code exp} claim is strictly after the current instant
     *       (i.e. the token has not yet expired).</li>
     * </ul>
     *
     * <p>Note: JJWT already throws a {@link io.jsonwebtoken.ExpiredJwtException} during
     * {@link #extractAllClaims(String)} for tokens whose {@code exp} has passed. This
     * explicit check is a defensive guard for edge cases where a pre-parsed Claims object
     * is used, and makes the contract of this method unambiguous.
     *
     * @param token       the compact JWT string to validate
     * @param userDetails the principal loaded from the database
     * @return {@code true} if the token is authentic and unexpired; {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        boolean usernameMatches = username.equals(userDetails.getUsername());
        boolean notExpired = extractExpiration(token).after(Date.from(Instant.now()));
        return usernameMatches && notExpired;
    }
}
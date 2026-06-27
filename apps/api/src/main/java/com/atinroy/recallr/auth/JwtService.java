package com.atinroy.recallr.auth;

import com.atinroy.recallr.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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
            @Value("${spring.application.security.jwt.expiration-seconds.access-token}") long jwtExpirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationSeconds = jwtExpirationSeconds;
    }

    public String buildToken(String subject, Date issuedAt, Date expiresAt, String tokenType) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .claim("token_type", tokenType)
                .signWith(signingKey)
                .compact();
    }

    public String generateAccessToken(CustomUserDetails principal) {
        Instant now = Instant.now();
        return buildToken(principal.getUsername(), Date.from(now), Date.from(now.plusSeconds(jwtExpirationSeconds)), "access_token");
    }

    public Claims extractAllClaims(String token) {
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

    public boolean isTokenValid(Claims claims, UserDetails userDetails) {
        // Expiry is enforced by extractAllClaims (parseSignedClaims throws ExpiredJwtException).
        return claims.getSubject().equals(userDetails.getUsername());
    }
}
package com.atinroy.recallr.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the {@code Set-Cookie} header value for the HttpOnly refresh-token cookie.
 * Cookie attributes (name, path, SameSite, Secure) are externalised to
 * {@code application.yaml} so dev (http, SameSite=Lax) and prod (https, SameSite=None)
 * can differ without code changes.
 */
@Component
public class RefreshTokenCookie {

    @Getter
    private final String name;
    private final String path;
    private final String sameSite;
    private final boolean secure;
    private final long maxAgeSeconds;

    public RefreshTokenCookie(
            @Value("${spring.application.security.jwt.cookie.name}") String name,
            @Value("${spring.application.security.jwt.cookie.path}") String path,
            @Value("${spring.application.security.jwt.cookie.same-site}") String sameSite,
            @Value("${spring.application.security.jwt.cookie.secure}") boolean secure,
            @Value("${spring.application.security.jwt.expiration-seconds.refresh-token}") long maxAgeSeconds
    ) {
        this.name = name;
        this.path = path;
        this.sameSite = sameSite;
        this.secure = secure;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /** Builds the {@code Set-Cookie} for a freshly issued refresh token. */
    public ResponseCookie build(String rawToken) {
        return ResponseCookie.from(name, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }

    /** Builds a {@code Set-Cookie} that instructs the browser to delete the cookie immediately. */
    public ResponseCookie clear() {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
    }
}

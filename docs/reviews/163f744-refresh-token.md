# Code Review — JWT Authentication with Refresh Tokens
**Commit:** `163f744` · **Branch:** `dev` · **Reviewer:** Principal Backend / Spring Security  
**Scope:** `apps/api/src/main/java/com/atinroy/recallr/auth/` · `security/` · `global/` · `application.yaml`  
**Working-tree staged files also reviewed:** `RefreshToken.java`, `RefreshTokenService.java`, updated `AuthService.java`, `JwtService.java` (buildToken / generateAccessToken split), `LoginResponse.java`

---

## TL;DR

The architecture is fundamentally sound — you made several genuinely good decisions (UUID as JWT subject, hash-stored refresh tokens, `@Transactional` on register, `CustomUserDetails` decoupled from email). But there are **10 confirmed bugs**, two of which are critical security vulnerabilities that would immediately compromise a production deployment.

---

## 1. Educational Breakdown

### 1.1 How Spring Security's filter chain relates to `@ControllerAdvice`

This is the most important conceptual point in this review.

`@RestControllerAdvice` (your `GlobalControllerAdvice`) only intercepts exceptions that escape from **Spring MVC dispatching** — i.e., exceptions thrown by `@Controller` methods and their service calls *after* the DispatcherServlet takes over. It has nothing to do with the Security filter chain.

Spring Security runs **before** DispatcherServlet in a separate filter chain (`FilterChainProxy`). Exceptions from within the security filters are handled by Spring Security's own exception translation infrastructure (`ExceptionTranslationFilter`), which calls `AuthenticationEntryPoint` for 401s and `AccessDeniedHandler` for 403s.

**The gap this creates in your code:** `authenticationManager.authenticate()` is called from `AuthService.login()`, which is called from `AuthController.login()` — this IS in the MVC layer, so `BadCredentialsException` escapes to `GlobalControllerAdvice`. But `GlobalControllerAdvice` has no handler for `BadCredentialsException`. Spring Boot's fallback maps it to **HTTP 500**.

Meanwhile, for requests to protected endpoints with no/invalid token, the security filter handles the 401 path — but without an `AuthenticationEntryPoint` configured, Spring Security 6 defaults to returning **HTTP 403** with a plain-text body.

**Mental model:** Think of two separate "error handling worlds" separated by the DispatcherServlet boundary. You need to configure both.

### 1.2 Why `getUsername()` returning UUID (not email) is correct

You made a deliberate and correct decision here: the JWT `sub` claim contains the user's UUID, and `CustomUserDetails.getUsername()` returns `id.toString()`. This allows OAuth users with no email to have tokens. The potential confusion: `UserDetailsService.loadUserByUsername(String)` is still called with the **email** during login (by `DaoAuthenticationProvider`), while `loadUserById(UUID)` is used by the JWT filter. These are different code paths for different purposes. The naming is misleading (`loadUserByUsername` takes an email) but the implementation is correct.

### 1.3 What `getReferenceById` actually does

`getReferenceById(id)` returns a **Hibernate proxy** — a shell object with only the ID field populated. The real database `SELECT` is deferred until you access a non-ID field. This is fine when you're going to pass the object to a relationship (avoiding the SELECT entirely) but it is wrong when you immediately access the entity's fields, as `UserMapper.toResponse(User user)` does. By contrast, `findById(id)` issues an eager `SELECT` immediately and returns a fully initialized entity.

The deeper problem: `UserMapper.toResponse(CustomUserDetails)` already exists and uses the in-memory principal — no database call at all. The `getReferenceById` path is redundant.

### 1.4 Why OSIV is a trap

Spring Boot's `spring.jpa.open-in-view=true` (the default) keeps a Hibernate `Session` open for the entire HTTP request lifecycle — including after a `@Transactional` method returns. This means lazy `@OneToMany` collections can be loaded at any point in the request. It works, but it hides bugs: any lazy-load that happens outside a transaction is a silent N+1 query with no warning. When you or a teammate eventually sets `spring.jpa.open-in-view: false` (which is the right call for a stateless API), code that relies on OSIV-backed lazy loading **crashes with `LazyInitializationException`**. Your `loadUserByUsername` and `loadUserById` methods both access the lazy `providers` collection without a `@Transactional` boundary.

---

## 2. Security & Robustness Issues

### [CRITICAL] Finding 1 — JWT signing secret committed to source control
**File:** `apps/api/src/main/resources/application.yaml:6`

```yaml
secret: "zM4y6Q1maI/mvNG7N3/pJyGR+2G0+pH/EbhEgZgX6AA="
```

The secret is embedded in a tracked file. Every developer, CI runner, and anyone who has ever cloned the repo permanently holds this key. Rotating it requires `git filter-branch` or BFG to rewrite history — and that rewrite never reaches every clone.

Additionally, this key decodes to exactly 32 bytes (256 bits) — the bare minimum for HS256 with zero margin. Production keys should be at least 64 bytes (512 bits), generated from a CSPRNG.

**Fix:**
```yaml
# application.yaml
spring:
  application:
    security:
      jwt:
        secret: "${JWT_SECRET}"   # no default — startup fails loudly if unset
        expiration-seconds:
          access-token: 900       # 15 minutes
          refresh-token: 1209600  # 14 days
```
Generate the secret: `openssl rand -base64 64`

---

### [CRITICAL] Finding 2 — Refresh token accepted as Bearer access token (token-type confusion)
**Files:** `JwtService.java:33`, `JwtAuthenticationFilter.java:60`

`RefreshTokenService.generateRefreshToken()` calls `jwtService.buildToken()` — the exact same method and signing key used for access tokens. The only structural difference is the `exp` claim value. `JwtAuthenticationFilter` calls `jwtService.isTokenValid()`, which checks only `sub` (matches) and `exp` (not expired) — it never consults `RefreshTokenRepository`.

**Consequence:** A 14-day refresh token placed in `Authorization: Bearer <refresh_token>` fully authenticates any API request. More critically: when you implement logout and call `revokeToken()`, the DB revocation has zero effect on the security filter path. A "revoked" refresh token continues to authenticate API requests for up to 14 days.

**Fix:** Add a `typ` claim to distinguish token types, and reject non-access tokens in the filter.

```java
// JwtService.java
public String buildToken(String subject, Date issuedAt, Date expiresAt, String tokenType) {
    return Jwts.builder()
            .subject(subject)
            .issuedAt(issuedAt)
            .expiration(expiresAt)
            .claim("typ", tokenType)   // "access" or "refresh"
            .signWith(signingKey)
            .compact();
}

public String generateAccessToken(CustomUserDetails principal) {
    Instant now = Instant.now();
    return buildToken(principal.getUsername(), Date.from(now),
            Date.from(now.plusSeconds(accessTokenExpirationSeconds)), "access");
}
```

```java
// JwtAuthenticationFilter.java — inside the try block, before setAuthentication:
String tokenType = jwtService.extractClaim(jwt, c -> c.get("typ", String.class));
if (!"access".equals(tokenType)) {
    // Refresh token presented as Bearer — reject silently
    filterChain.doFilter(request, response);
    return;
}
```

---

### [HIGH] Finding 3 — No refresh token rotation
**File:** `AuthService.java:76-81`

```java
return new LoginResponse(accessToken, token, "Bearer", ...);
//                                    ^^^^^ original token returned unchanged
```

`revokeToken()` is never called. The original refresh token is returned as-is. This means:
- An exfiltrated refresh token can mint access tokens indefinitely for 14 days
- There is no way to detect token theft (RTR's "reuse detection" signal never fires)

**Fix:** Rotate on every use — revoke the incoming token, issue a new one.

```java
public LoginResponse refreshToken(String token) {
    if (!refreshTokenService.isTokenValid(token)) {
        throw new InvalidTokenException("Token is not valid");
    }

    String userId = jwtService.extractUsername(token);
    UUID uuid = UUID.fromString(userId);
    CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserById(uuid);

    // Rotate: revoke old, issue new
    refreshTokenService.revokeToken(token);
    String newRefreshToken = refreshTokenService.generateRefreshToken(principal);
    String accessToken = jwtService.generateAccessToken(principal);

    return new LoginResponse(accessToken, newRefreshToken, "Bearer", UserMapper.toResponse(principal));
}
```

For full RTR with theft detection: if the same refresh token is presented twice (after it was already rotated), that's a compromise signal — revoke the entire user's refresh token family.

---

### [HIGH] Finding 4 — `BadCredentialsException` → HTTP 500 on wrong credentials
**File:** `GlobalControllerAdvice.java` (missing handler)

`authenticationManager.authenticate()` throws `BadCredentialsException` when credentials are wrong. `GlobalControllerAdvice` has no handler for it. Spring Boot maps the uncaught `RuntimeException` to HTTP 500.

**Fix:** Add a handler for `AuthenticationException` (catches `BadCredentialsException`, `DisabledException`, `LockedException`, etc.):

```java
// GlobalControllerAdvice.java
@ExceptionHandler(AuthenticationException.class)
public ProblemDetail handleAuthenticationFailure(AuthenticationException e) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
            "Authentication failed");
    detail.setTitle("Unauthorized");
    return detail;
}
```

Note the deliberate vague message — never expose *why* authentication failed (wrong email vs wrong password leaks user enumeration).

---

### [HIGH] Finding 5 — `loadUserById`/`loadUserByUsername` not `@Transactional` → LazyInitializationException
**File:** `CustomUserDetailsService.java:24,46`

Both methods call `userRepository.findById()` (Spring Data runs this in its own short read transaction, then commits and detaches the entity). The very next line accesses `user.getProviders()`, which is a `@OneToMany(fetch=LAZY)` collection on a **now-detached** entity. This works today only because OSIV defaults to `true`. Setting `spring.jpa.open-in-view: false` — the correct setting for a stateless REST API — causes **every authenticated request** to throw `LazyInitializationException`.

**Fix:**

```java
// CustomUserDetailsService.java
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    // ... existing implementation unchanged
}

@Transactional(readOnly = true)
public UserDetails loadUserById(@NonNull UUID id) {
    // ... existing implementation unchanged
}
```

Add `spring.jpa.open-in-view: false` to `application.yaml` to enforce the correct discipline and catch any remaining OSIV dependencies now, not in production.

---

### [HIGH] Finding 6 — `UsernameNotFoundException` escapes `JwtAuthenticationFilter` catch → HTTP 500
**File:** `JwtAuthenticationFilter.java:75`

```java
} catch (JwtException | IllegalArgumentException e) {
    SecurityContextHolder.clearContext();
}
```

`UsernameNotFoundException` extends `AuthenticationException` extends `RuntimeException`. It is **not** a `JwtException` or `IllegalArgumentException`. When a valid JWT contains the UUID of a deleted user, `loadUserById()` throws `UsernameNotFoundException` — it escapes the catch block, `filterChain.doFilter()` is never called, and Spring returns HTTP 500.

**Fix:** Catch `UsernameNotFoundException` (or its parent) explicitly:

```java
} catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
    SecurityContextHolder.clearContext();
    // fall through to filterChain.doFilter — Spring Security will return 401
}
```

---

### [HIGH] Finding 7 — `getReferenceById()` in `login()` and `refreshToken()` when `UserMapper.toResponse(principal)` exists
**File:** `AuthService.java:62,80`

```java
UserMapper.toResponse(userRepository.getReferenceById(principal.getId()))
```

`UserMapper.toResponse(CustomUserDetails)` already exists at `UserMapper.java:38` and reads `id`, `email`, and `authorities` directly from the in-memory principal — no database call. The `getReferenceById()` call creates a Hibernate proxy that triggers a redundant `SELECT` to re-fetch data already in memory, and crashes with `LazyInitializationException` when OSIV is disabled.

**Fix:** One-line change in both places:

```java
// login() line 62 and refreshToken() line 80
UserMapper.toResponse(principal)   // already in memory, no DB needed
```

---

### [MEDIUM] Finding 8 — `/auth/me` under `permitAll()` wildcard → NPE/500 for unauthenticated callers
**Files:** `SecurityConfig.java:38`, `AuthController.java:51`

`.requestMatchers("/auth/**").permitAll()` matches `/auth/me`. `permitAll()` only affects the authorization decision — it does not skip the authentication filter. When a request arrives with no (or invalid) token, the filter does not populate the `SecurityContext`, but the request is still allowed through. Spring Security injects `null` for `@AuthenticationPrincipal CustomUserDetails principal`. `UserMapper.toResponse(null)` throws `NullPointerException` — HTTP 500 instead of 401.

The endpoint's own Javadoc says *"a 401 means the token has expired"* — a guarantee it cannot keep.

**Fix:** Enumerate only the truly public endpoints rather than using a wildcard:

```java
.authorizeHttpRequests(req -> req
    .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh").permitAll()
    .anyRequest().authenticated()
)
```

---

### [MEDIUM] Finding 9 — No `AuthenticationEntryPoint` → 403 Forbidden instead of 401 Unauthorized
**File:** `SecurityConfig.java` (missing `.exceptionHandling()` block)

Without form login, HTTP Basic, or an explicit `AuthenticationEntryPoint`, Spring Security 6 defaults to `Http403ForbiddenEntryPoint` — returning `403 Forbidden` with no JSON body for all unauthenticated requests to protected endpoints. REST API consumers typically treat 401 as "re-authenticate" and 403 as "you don't have permission" — returning 403 for a missing token breaks client retry logic.

**Fix:**

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
    })
)
```

---

### [MEDIUM] Finding 10 — Three JJWT parse+verify cycles per authenticated request
**Files:** `JwtAuthenticationFilter.java:52,60`, `JwtService.java:87,89`

Every authenticated request pays for three full HMAC-SHA signature verifications:

1. `jwtService.extractUsername(jwt)` → `extractAllClaims()` → `parseSignedClaims()` (**parse #1**)
2. Inside `isTokenValid()`: `extractUsername(token)` → `extractAllClaims()` → `parseSignedClaims()` (**parse #2**)
3. Inside `isTokenValid()`: `extractExpiration(token)` → `extractAllClaims()` → `parseSignedClaims()` (**parse #3**)

Note: JJWT already throws `ExpiredJwtException` during parse #1 if the token is expired — the explicit `extractExpiration().after(now)` check in `isTokenValid()` is therefore dead code (it can never catch an expiry that JJWT didn't already throw on).

**Fix:** Parse once, return a `Claims` object, read both fields from it:

```java
// JwtService.java — replace the filter's two-call pattern with one:
public Claims extractAndValidateClaims(String token) {
    // Throws JwtException (including ExpiredJwtException) on any problem
    return extractAllClaims(token);  // single parse+verify
}

// isTokenValid becomes a Claims-based check — no re-parsing:
public boolean isTokenValid(Claims claims, UserDetails userDetails) {
    return claims.getSubject() != null
            && claims.getSubject().equals(userDetails.getUsername());
    // Expiry already enforced by JJWT during parsing — no explicit check needed
}
```

```java
// JwtAuthenticationFilter.java
try {
    Claims claims = jwtService.extractAndValidateClaims(jwt);  // one parse
    String tokenType = claims.get("typ", String.class);
    if (!"access".equals(tokenType)) {
        filterChain.doFilter(request, response);
        return;
    }
    String userId = claims.getSubject();
    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserById(UUID.fromString(userId));
        if (jwtService.isTokenValid(claims, userDetails)) {  // no re-parse
            // ... set auth token
        }
    }
} catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
    SecurityContextHolder.clearContext();
}
```

---

## 3. Industry Best Practices Assessment

### 3.1 What you got right ✓

| Decision | Why it's good |
|---|---|
| UUID as JWT `sub` (not email) | Email can change; UUID is stable. Decouples token from PII. |
| `RefreshToken.tokenHash` stores SHA-256 of token | Compromised DB does not expose raw refresh tokens. |
| `@Size(min=8, max=72)` on password with Bcrypt comment | Correct — bcrypt silently truncates beyond 72 bytes. |
| `@Transactional` on `register()` | Correct — prevents partial saves. |
| `SessionCreationPolicy.STATELESS` | Correct for a JWT-based API. |
| `CustomUserDetails.from(User, String)` factory | Clean separation from the JPA entity. |
| `loadUserByUsername` normalizes email (`.toLowerCase().strip()`) | Prevents case-sensitivity login failures. |
| `DaoAuthenticationProvider(userDetailsService)` constructor injection | Correct Spring Security 6 pattern. |

### 3.2 Refresh Token Rotation (RTR)

**Current state:** The `revokeToken()` method exists in `RefreshTokenService` but is never called from `AuthService.refreshToken()`. The old token is returned unchanged on every refresh. There is no rotation.

**Industry standard (RFC 6749 + OAuth 2.0 Security BCP):**
- Every `/auth/refresh` call must issue a **new** refresh token and revoke the old one
- If the same refresh token is presented a second time (after it was already rotated), treat it as a compromise signal: revoke the entire token family for that user and force re-login
- Refresh token lifetime resets on each rotation (sliding window) or is fixed (absolute TTL) — both are acceptable, fixed is simpler

### 3.3 Token Storage

**Access token:** Short-lived (15 min) — memory storage in the SPA is acceptable. `Authorization: Bearer` header is the correct delivery mechanism. ✓

**Refresh token:** Long-lived (14 days) — returning it in a JSON body means the client must store it somewhere. The safe options are:

| Storage | XSS risk | CSRF risk | Recommended? |
|---|---|---|---|
| `localStorage` | ✗ High | None | No |
| `HttpOnly; Secure; SameSite=Strict` cookie | None | Low (mitigated by SameSite) | **Yes** |
| In-memory (JS var) | Low (gone on refresh) | None | Yes for SPAs that can tolerate it |

Returning the refresh token in the JSON response body and trusting the client to store it securely is the pattern that loses to XSS. Consider moving to `HttpOnly` cookie delivery — the `/auth/refresh` endpoint can then read the cookie server-side with no JS access possible.

### 3.4 Expiration Management

**Access token (15 min):** Correct. Short enough to limit blast radius.

**Refresh token (14 days):** Reasonable for a consumer app, long for a security-sensitive one. Pair with rotation and absolute expiry (never extend beyond the original issue date) for the best security posture.

**Missing:** There is no `/auth/logout` endpoint reachable from HTTP. `RefreshTokenService.revokeToken()` and `deleteByUserId()` exist but no controller endpoint calls them. A stolen device stays authenticated until the refresh token naturally expires.

### 3.5 Production Readiness Score

| Dimension | Score | Notes |
|---|---|---|
| Core auth flow correctness | 7/10 | Works, but 2 critical bugs in error handling |
| Token security | 4/10 | Secret committed, type confusion, no rotation |
| Exception handling | 3/10 | Wrong credentials returns 500, auth failures return 403 |
| Transaction discipline | 5/10 | OSIV dependency hides LazyInit bugs |
| Performance | 5/10 | 3× JWT parses per request, redundant DB proxy |
| Architecture / extensibility | 8/10 | Clean separation, good design for OAuth extension |

---

## 4. Refactored Code

### 4.1 `SecurityConfig.java` — Add `AuthenticationEntryPoint`, narrow `permitAll()`

```java
package com.atinroy.recallr.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        // Enumerate public endpoints explicitly — never use /auth/** wildcard
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // Return JSON 401 for unauthenticated requests — not the default 403 plain-text
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            objectMapper.writeValue(response.getWriter(),
                                    Map.of("status", HttpStatus.UNAUTHORIZED.value(),
                                           "error", "Unauthorized",
                                           "message", "Authentication required"));
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### 4.2 `JwtService.java` — Single parse per call, `typ` claim, no redundant expiry check

```java
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

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String TYPE_ACCESS  = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final long accessTokenExpirationSeconds;
    private final SecretKey signingKey;

    public JwtService(
            @Value("${spring.application.security.jwt.secret}") String secret,
            @Value("${spring.application.security.jwt.expiration-seconds.access-token}") long accessTokenExpirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    // Package-private: used by RefreshTokenService to build refresh tokens with the same key
    String buildToken(String subject, Date issuedAt, Date expiresAt, String tokenType) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .claim(CLAIM_TOKEN_TYPE, tokenType)   // distinguish access vs refresh
                .signWith(signingKey)
                .compact();
    }

    public String generateAccessToken(CustomUserDetails principal) {
        Instant now = Instant.now();
        return buildToken(principal.getUsername(), Date.from(now),
                Date.from(now.plusSeconds(accessTokenExpirationSeconds)), TYPE_ACCESS);
    }

    // Single parse — throws JwtException (including ExpiredJwtException) on any problem.
    // JJWT already enforces expiry here; callers do not need to re-check.
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    // Claims-based validation — no re-parsing, no redundant expiry check
    public boolean isTokenValid(Claims claims, UserDetails userDetails) {
        String subject = claims.getSubject();
        return subject != null && subject.equals(userDetails.getUsername());
        // Expiry is already enforced by JJWT during parseAndValidate() — no explicit check needed
    }

    // Kept for use in AuthService.refreshToken() to extract the userId from a refresh token
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseAndValidate(token));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
}
```

---

### 4.3 `JwtAuthenticationFilter.java` — Single parse, type check, catch `UsernameNotFoundException`

```java
package com.atinroy.recallr.security;

import com.atinroy.recallr.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            // Single parse — signature verified, expiry enforced, claims returned
            Claims claims = jwtService.parseAndValidate(jwt);

            // Reject refresh tokens presented as Bearer credentials
            if (!jwtService.isAccessToken(claims)) {
                filterChain.doFilter(request, response);
                return;
            }

            String userId = claims.getSubject();
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserById(UUID.fromString(userId));

                // Claims-based check — no re-parsing
                if (jwtService.isTokenValid(claims, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Malformed token, expired token, or deleted user — clear context and proceed.
            // The downstream AuthenticationEntryPoint will return 401.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
```

---

### 4.4 `CustomUserDetailsService.java` — Add `@Transactional(readOnly = true)` to both methods

```java
package com.atinroy.recallr.security;

import com.atinroy.recallr.user.IdentityProvider;
import com.atinroy.recallr.user.User;
import com.atinroy.recallr.user.UserProvider;
import com.atinroy.recallr.user.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)   // keeps session open for the providers collection access
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase().strip())
                .orElseThrow(() -> new UsernameNotFoundException(email));
        String password = user.getProviders().stream()
                .filter(p -> p.getProvider() == IdentityProvider.LOCAL)
                .findFirst()
                .map(UserProvider::getPasswordHash)
                .orElseThrow(() -> new UsernameNotFoundException("No local login for this account"));
        return CustomUserDetails.from(user, password);
    }

    @Transactional(readOnly = true)   // same reason — lazy providers must be loaded within a session
    public UserDetails loadUserById(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        String password = user.getProviders().stream()
                .filter(p -> p.getProvider() == IdentityProvider.LOCAL)
                .findFirst()
                .map(UserProvider::getPasswordHash)
                .orElse(null);
        return CustomUserDetails.from(user, password);
    }
}
```

---

### 4.5 `AuthService.java` — Fix `getReferenceById`, add rotation, handle `BadCredentialsException`

```java
package com.atinroy.recallr.auth;

import com.atinroy.recallr.auth.dto.LoginResponse;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.auth.dto.LoginRequest;
import com.atinroy.recallr.security.CustomUserDetails;
import com.atinroy.recallr.security.CustomUserDetailsService;
import com.atinroy.recallr.user.*;
import com.atinroy.recallr.user.dto.UserResponse;
import com.atinroy.recallr.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    @Transactional
    public UserResponse register(EmailRegisterRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }
        String passwordHash = passwordEncoder.encode(userRequest.password());
        User user = UserMapper.toEntity(userRequest, passwordHash);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    // AuthenticationException (BadCredentialsException, etc.) propagates to GlobalControllerAdvice
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.generateRefreshToken(principal);

        // Use the in-memory principal — no extra DB round-trip via getReferenceById
        return new LoginResponse(accessToken, refreshToken, "Bearer", UserMapper.toResponse(principal));
    }

    @Transactional
    public LoginResponse refreshToken(String token) {
        if (!refreshTokenService.isTokenValid(token)) {
            throw new InvalidTokenException("Token is not valid");
        }

        String userId = jwtService.extractUsername(token);
        UUID uuid = UUID.fromString(userId);
        CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserById(uuid);

        // Rotate: revoke the incoming token and issue a fresh one
        refreshTokenService.revokeToken(token);
        String newRefreshToken = refreshTokenService.generateRefreshToken(principal);
        String accessToken = jwtService.generateAccessToken(principal);

        return new LoginResponse(accessToken, newRefreshToken, "Bearer", UserMapper.toResponse(principal));
    }
}
```

---

### 4.6 `GlobalControllerAdvice.java` — Add `AuthenticationException` handler, migrate to `ProblemDetail`

```java
package com.atinroy.recallr.global;

import com.atinroy.recallr.auth.InvalidTokenException;
import com.atinroy.recallr.user.EmailAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ProblemDetail handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidToken(InvalidTokenException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    // Handles BadCredentialsException, DisabledException, LockedException, etc.
    // Vague message is intentional — never distinguish "wrong email" from "wrong password"
    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationFailure(AuthenticationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }
}
```

---

### 4.7 `application.yaml` — Remove hardcoded secret, add OSIV:false

```yaml
spring:
  application:
    name: api
    security:
      jwt:
        secret: "${JWT_SECRET}"        # inject via env var — startup fails loudly if absent
        expiration-seconds:
          access-token: 900            # 15 minutes
          refresh-token: 1209600       # 14 days
  jmx:
    enabled: false
  jpa:
    open-in-view: false                # makes lazy-load bugs surface immediately, not in prod
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  mvc:
    servlet:
      path: /api/v1
```

**Generate a secure key locally for `.env` / your secrets manager:**
```bash
export JWT_SECRET=$(openssl rand -base64 64)
```

---

## Summary Table

| # | Severity | File | Issue | Fix |
|---|---|---|---|---|
| 1 | CRITICAL | `application.yaml:6` | Secret hardcoded in git | Env var injection |
| 2 | CRITICAL | `JwtService.java`, `JwtAuthenticationFilter.java` | Refresh token accepted as Bearer (type confusion) | Add `typ` claim, check in filter |
| 3 | HIGH | `AuthService.java:77` | No refresh token rotation | Revoke + reissue on each refresh |
| 4 | HIGH | `GlobalControllerAdvice.java` | `BadCredentialsException` → HTTP 500 | Add `AuthenticationException` handler |
| 5 | HIGH | `CustomUserDetailsService.java:24,46` | No `@Transactional` → OSIV dependency | Add `@Transactional(readOnly=true)` |
| 6 | HIGH | `JwtAuthenticationFilter.java:75` | `UsernameNotFoundException` escapes catch → HTTP 500 | Add to catch clause |
| 7 | HIGH | `AuthService.java:62,80` | `getReferenceById()` when `toResponse(principal)` exists | Use `UserMapper.toResponse(principal)` |
| 8 | MEDIUM | `SecurityConfig.java:38`, `AuthController.java:51` | `/auth/**` wildcard → `/auth/me` NPE/500 | Enumerate explicit public paths |
| 9 | MEDIUM | `SecurityConfig.java` | No `AuthenticationEntryPoint` → 403 not 401 JSON | Configure entry point |
| 10 | MEDIUM | `JwtAuthenticationFilter.java` + `JwtService.java` | 3× JJWT parse per request | Parse once, pass `Claims` object |

# Code Review — Complete Auth Implementation
**Commit:** `8908d01` · **Branch:** `dev` · **Reviewer:** Principal Backend / Spring Security  
**Scope:** `apps/api/src/main/java/com/atinroy/recallr/auth/` · `security/` · `global/` · `user/` · `common/` · `application.yaml` · `db/migration/`  
**Range:** `main...HEAD` (full auth feature branch — all files new)

---

## TL;DR

The previous findings (secret in SCM, token-type confusion, no rotation, BadCredentials→500, OSIV trap) are **all fixed**. The architecture is production-grade: RTR with reuse detection, UUID-as-sub, SHA-256 hashed tokens, `Persistable` optimization on `BaseEntity`. There are **10 new findings**, one of which breaks the entire token rotation invariant under concurrent load and must be fixed before this merges.

---

## 1. Educational Breakdown

### 1.1 Why READ COMMITTED breaks Refresh Token Rotation under concurrency

Postgres's default isolation is `READ COMMITTED`. In this mode, every statement sees the latest committed snapshot at the moment it runs — not the snapshot at transaction start. Two concurrent transactions that both read the same `refresh_tokens` row before either commits will both see `is_revoked = false`. There is no conflict unless one of them holds a row-level lock.

Without a `SELECT … FOR UPDATE` (which `@Lock(PESSIMISTIC_WRITE)` produces in Spring Data JPA), two simultaneous `POST /auth/refresh` requests with the same token will both pass the revocation guard, both call `revokeToken` and `generateRefreshToken`, and both commit successfully. The client now holds two independent, valid refresh tokens derived from one original — the rotation invariant is broken silently, with no exception thrown.

The fix is one annotation: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on `findByTokenHash`. This serializes concurrent refresh attempts for the same token. The second request blocks until the first commits, then reads `is_revoked = true` and triggers reuse detection correctly.

### 1.2 Why `@Transactional` on `login()` matters even though you're not writing to a "login" table

Spring's `@Transactional` with default `REQUIRED` propagation means: if a transaction already exists, join it; if not, start a new one. `generateRefreshToken()` is `@Transactional(REQUIRED)`. When called from an outer method that is **not** `@Transactional`, it starts and **commits** its own standalone transaction immediately. The refresh token row is durably written to the database before control returns to `login()`.

Everything `login()` does after that point — `jwtService.generateAccessToken(principal)`, constructing the `LoginResponse` — runs **outside** any transaction. If any of it fails, the refresh token in the DB cannot be rolled back. The client receives a 500; the refresh token silently sits in the DB undelivered and unrevocable.

The fix — `@Transactional` on `login()` — makes `generateRefreshToken()` join the outer transaction instead of committing early. The whole login succeeds or fails as a unit.

### 1.3 Why the `notExpired` check in `isTokenValid` is always true

`JwtAuthenticationFilter` calls `jwtService.extractAllClaims(jwt)` to produce the `Claims` object. Inside `extractAllClaims`, JJWT's `parseSignedClaims()` validates the JWT's `exp` claim against the current clock as part of parsing. If the token is expired, it throws `ExpiredJwtException` — a subclass of `JwtException` — before returning. The `catch (JwtException …)` block at line 86 of the filter catches it, clears the security context, and lets the request continue unauthenticated.

This means execution can only ever reach `isTokenValid(claims, userDetails)` with a `Claims` object that JJWT has already certified as non-expired. The line `boolean notExpired = claims.getExpiration().after(Date.from(Instant.now()));` is dead code — it can never evaluate to `false`. It misleads future readers into believing `isTokenValid` provides an independent expiry guard, which it does not.

### 1.4 Why `token_hash` needs a UNIQUE constraint in the migration, not just on the entity

`@Column(unique = true)` on a JPA entity field causes Hibernate to create a unique constraint in the database — but **only** when `ddl-auto` is `create`, `create-drop`, or `update`. Your `application.yaml` sets `ddl-auto: validate`. In validate mode, Hibernate checks that the column exists with the right type and nullability, but it does not create or modify any constraints.

This means the `UNIQUE` invariant that `findByTokenHash` and your revocation logic depend on is **not enforced at the database level**. If a bug, migration error, or future code path inserts two rows with the same hash, `findByTokenHash` would throw `IncorrectResultSizeDataAccessException` (a 500) because `findBy…` returns an `Optional` but Spring Data throws if the result set has more than one row. The fix is a `CONSTRAINT` clause in `V2__refresh_tokens.sql`.

---

## 2. Security & Robustness Issues

### [CRITICAL] Finding 1 — Concurrent refresh breaks token rotation invariant
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/RefreshTokenRepository.java:13`

```java
Optional<RefreshToken> findByTokenHash(String tokenHash);
// No @Lock annotation — issues a plain SELECT with no row-level lock
```

Two simultaneous requests with the same token both read `is_revoked = false` under READ COMMITTED isolation, both pass the guard in `getRefreshToken()`, and both proceed to revoke the old token and generate a new one. The user ends up with two independent active refresh tokens.

This also breaks reuse detection: if an attacker races their stolen token against the legitimate client's concurrent refresh, both may succeed, producing two valid token families with no alert fired.

**Fix:** Add `@Lock` to the repository method:

```java
// RefreshTokenRepository.java
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<RefreshToken> findByTokenHash(String tokenHash);
```

This issues `SELECT … FOR UPDATE`, serializing concurrent lookups of the same row. The second concurrent request blocks, then reads `is_revoked = true`, and reuse detection fires correctly.

---

### [HIGH] Finding 2 — `JwtException` uncaught in `AuthService.refreshToken` → HTTP 500
**Files:** `AuthService.java:69`, `GlobalControllerAdvice.java` (missing handler)

```java
// AuthService.java
RefreshToken refreshToken = refreshTokenService.getRefreshToken(token); // line 67
String userId = jwtService.extractUsername(token);                      // line 69 — can throw JwtException
```

`extractUsername` calls `extractAllClaims` → `parseSignedClaims`. JJWT throws `JwtException` (e.g., `ExpiredJwtException`, `SignatureException`) for invalid or expired tokens. `GlobalControllerAdvice` handles `InvalidTokenException`, `BadCredentialsException`, `AuthenticationException`, and `EmailAlreadyExistsException` — but not `JwtException`. The exception propagates to Spring Boot's default error handler and returns HTTP 500.

This path is reachable in two scenarios:
1. Clock skew: the DB expiry check passes, but JJWT's internal clock considers the JWT expired milliseconds later.
2. Key rotation: if the signing key is rotated without flushing DB records, every existing refresh token's hash is still in the DB but `parseSignedClaims` throws `SignatureException`.

Note: the `userId` needed here is already available on the entity as `refreshToken.getUserId()` — the entire JWT re-parse is redundant. Eliminating it removes this failure mode entirely.

**Fix (preferred):** Use the entity's `userId` field directly instead of re-parsing the JWT:

```java
// AuthService.java
@Transactional
public LoginResponse refreshToken(String token) {
    RefreshToken refreshToken = refreshTokenService.getRefreshToken(token);

    UUID uuid = refreshToken.getUserId();  // no re-parse needed
    CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserById(uuid);

    refreshTokenService.revokeToken(refreshToken);
    // ...
}
```

**Fix (belt-and-suspenders):** Also add to `GlobalControllerAdvice`:

```java
@ExceptionHandler(io.jsonwebtoken.JwtException.class)
public ResponseEntity<Map<String, Object>> handleJwtException(io.jsonwebtoken.JwtException e) {
    return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, "Invalid token"), HttpStatus.UNAUTHORIZED);
}
```

---

### [HIGH] Finding 3 — `RefreshRequest.refreshToken` has no `@NotBlank` → NPE → HTTP 500
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/dto/RefreshRequest.java:4`

```java
public record RefreshRequest(
        String refreshToken   // no @NotBlank, no @NotNull
) {}
```

`AuthController.refresh()` has `@Valid` on the parameter, but Bean Validation only enforces the constraints declared on the record's fields. With no constraint, `{ "refreshToken": null }` or a missing field passes validation and arrives at `RefreshTokenService.hash(null)`. Inside `hash()`:

```java
byte[] digest = MessageDigest.getInstance("SHA-256")
        .digest(token.getBytes(StandardCharsets.UTF_8));  // NullPointerException
```

The NPE propagates uncaught through the MVC layer and Spring Boot returns HTTP 500. The client should receive HTTP 400.

**Fix:**

```java
import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken
) {}
```

---

### [HIGH] Finding 4 — `token_hash` has no UNIQUE constraint and no index in the migration
**File:** `apps/api/src/main/resources/db/migration/V2__refresh_tokens.sql:6`

```sql
token_hash  VARCHAR(64) NOT NULL,
-- no UNIQUE constraint, no index
```

`ddl-auto: validate` means the `@Column(unique = true)` on the entity never creates a database constraint. Two effects:
1. **Correctness:** The uniqueness invariant used by `findByTokenHash` is not enforced at the database level. A duplicate row would cause `IncorrectResultSizeDataAccessException` (HTTP 500) on the next refresh attempt.
2. **Performance:** `findByTokenHash` is called on every `POST /auth/refresh`. Without an index, Postgres performs a full sequential scan of `refresh_tokens` on each request. Latency grows linearly with the number of issued tokens.

**Fix:** Add to `V2__refresh_tokens.sql`:

```sql
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked  BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
```

The `UNIQUE` constraint implicitly creates a B-tree index on `token_hash`. The `user_id` index accelerates `revokeAllByUserId`.

---

### [MEDIUM] Finding 5 — `AuthService.login()` has no `@Transactional` → leaked refresh token
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/AuthService.java:44`

```java
// AuthService.java — note: @Transactional is on register(), but NOT on login()
public LoginResponse login(LoginRequest request) {
    // ...
    String accessToken = jwtService.generateAccessToken(principal);
    String refreshToken = refreshTokenService.generateRefreshToken(principal);  // commits its own transaction here
    return new LoginResponse(accessToken, refreshToken, "Bearer", UserMapper.toResponse(principal));
}
```

`generateRefreshToken()` is `@Transactional(REQUIRED)`. With no outer transaction on `login()`, it starts and commits a standalone transaction before returning. The refresh token is durably persisted. If anything after that line throws — including `UserMapper.toResponse(principal)` — the refresh token is in the database and unrevocable, but the client receives a 500 and never gets the token.

`register()` correctly has `@Transactional`. `login()` should too.

**Fix:**

```java
@Transactional
public LoginResponse login(LoginRequest request) {
    // ... existing implementation unchanged
}
```

---

### [MEDIUM] Finding 6 — JWT `sub` not cross-validated against DB `userId` in `refreshToken()`
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/AuthService.java:67-71`

```java
RefreshToken refreshToken = refreshTokenService.getRefreshToken(token);  // looks up by hash → uses DB userId
String userId = jwtService.extractUsername(token);                        // extracts from JWT sub
UUID uuid = UUID.fromString(userId);
// userId (from JWT) is used to load the principal; refreshToken.getUserId() is never compared
```

The DB lookup and the JWT extraction are never cross-validated. Under normal operation they are guaranteed to match (the token was generated with `principal.getUsername()` as the JWT sub and `principal.getId()` as the DB `userId`). But they are two independent trust sources: the DB entry (from `findByTokenHash`) and the JWT claim (from `extractUsername`). If they diverge — via a manual DB edit, a SHA-256 hash collision, or a future refactor that splits token storage — the wrong user's access token is issued while the wrong user's refresh token is revoked.

If you accept Finding 2's preferred fix (use `refreshToken.getUserId()` directly), this finding is resolved as a side effect — there is only one source of truth.

**Fix (if keeping the JWT re-parse):**

```java
if (!refreshToken.getUserId().equals(uuid)) {
    throw new InvalidTokenException("Token identity mismatch");
}
```

---

### [MEDIUM] Finding 7 — `authException.getMessage()` concatenated into JSON without escaping
**File:** `apps/api/src/main/java/com/atinroy/recallr/security/SecurityConfig.java:65`

```java
response.getWriter().write(
    "{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}"
);
```

If `authException.getMessage()` contains a double-quote, backslash, or newline (e.g., `Full authentication is required to access resource "/api/v1/admin"`), the response body is invalid JSON. Strict JSON parsers on the client throw a parse error instead of extracting the error message.

This also uses a different schema (`{error, message}`) than every other error handler (`{timestamp, status, error, message}`), creating an inconsistency clients must special-case.

**Fix:** Inject `ObjectMapper` and use it, or delegate to Spring's `HandlerExceptionResolver`. Simplest option:

```java
// SecurityConfig.java
private final ObjectMapper objectMapper;

// In the entryPoint lambda:
response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
response.setContentType("application/json;charset=UTF-8");
Map<String, Object> body = Map.of(
    "timestamp", Instant.now().toString(),
    "status", 401,
    "error", "Unauthorized",
    "message", "Authentication required"
);
response.getWriter().write(objectMapper.writeValueAsString(body));
```

---

## 3. Code Quality Issues

### [MEDIUM] Finding 8 — Dead `notExpired` check in `JwtService.isTokenValid`
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/JwtService.java:68-71`

```java
public boolean isTokenValid(Claims claims, UserDetails userDetails) {
    String username = claims.getSubject();
    boolean usernameMatches = username.equals(userDetails.getUsername());
    boolean notExpired = claims.getExpiration().after(Date.from(Instant.now()));  // always true
    return usernameMatches && notExpired;
}
```

`Claims` is only ever produced by `extractAllClaims`, which calls `parseSignedClaims`. JJWT throws `ExpiredJwtException` for expired tokens during parsing, before the `Claims` object is returned. A `Claims` object with a past expiration can never be passed to `isTokenValid` through the current call paths.

The dead branch creates a false belief that `isTokenValid` independently protects against expired tokens. A future refactor that pre-fetches `Claims` from a cache and passes them without re-parsing would silently skip the real expiry guard, while this check would still evaluate to `true`.

**Fix:** Remove the `notExpired` branch and document why:

```java
public boolean isTokenValid(Claims claims, UserDetails userDetails) {
    // Expiry is enforced by extractAllClaims (parseSignedClaims throws ExpiredJwtException).
    // This method only needs to verify the subject matches the loaded principal.
    return claims.getSubject().equals(userDetails.getUsername());
}
```

---

### [LOW] Finding 9 — `GlobalControllerAdvice` returns two different error schemas for auth failures
**File:** `apps/api/src/main/java/com/atinroy/recallr/global/GlobalControllerAdvice.java:37-49`

```java
@ExceptionHandler(BadCredentialsException.class)
public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
    return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, e.getMessage()), HttpStatus.UNAUTHORIZED);
    // Returns: { timestamp, status, error, message }
}

@ExceptionHandler(AuthenticationException.class)
public ProblemDetail handleAuthenticationFailure(AuthenticationException e) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication failed");
    // Returns: { type, title, status, detail } — different schema
}
```

`BadCredentialsException` extends `AuthenticationException`. Spring MVC correctly selects the most specific handler for `BadCredentialsException`, so the two handlers don't conflict. But other `AuthenticationException` subtypes — `DisabledException`, `LockedException`, `AccountExpiredException` (all thrown by `DaoAuthenticationProvider` when account status checks fail) — fall through to the `ProblemDetail` handler with a structurally different JSON body. A client parsing the `{ timestamp, status, error, message }` envelope will silently fail to deserialize these responses.

**Fix:** Standardize on one schema for all auth failures, preferably reusing `errorBody()`:

```java
@ExceptionHandler(AuthenticationException.class)
public ResponseEntity<Map<String, Object>> handleAuthenticationFailure(AuthenticationException e) {
    return new ResponseEntity<>(errorBody(HttpStatus.UNAUTHORIZED, "Authentication failed"), HttpStatus.UNAUTHORIZED);
}
```

---

### [LOW] Finding 10 — `RefreshTokenRevocationService` is a silent REQUIRES_NEW trap
**File:** `apps/api/src/main/java/com/atinroy/recallr/auth/RefreshTokenRevocationService.java`

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void revokeAllTokensForUser(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId);
}
```

This class exists for one reason only: Spring cannot proxy `self-invocations` — a `@Transactional(REQUIRES_NEW)` method called from another method on the same bean is not intercepted by the AOP proxy and does not start a new transaction. The only way to get a truly independent transaction is to call it on a different bean.

The class name and method name carry no hint of this. A developer simplifying the auth package may inline `revokeAllByUserId()` back into `RefreshTokenService` as an obvious cleanup. The `REQUIRES_NEW` propagation silently disappears — revocations on reuse detection now participate in the outer transaction and roll back with it if `generateRefreshToken()` subsequently fails, leaving all stolen tokens un-revoked.

**Fix:** Add a comment explaining the constraint:

```java
/**
 * This class exists as a separate Spring bean solely to provide a REQUIRES_NEW transaction
 * boundary for revokeAllTokensForUser(). Spring AOP cannot proxy self-invocations, so calling
 * a @Transactional(REQUIRES_NEW) method from another method on the same bean has no effect.
 * Do NOT inline this into RefreshTokenService — it will silently lose the transaction isolation
 * that makes refresh token reuse detection safe.
 */
@Service
public class RefreshTokenRevocationService { ... }
```

---

## 4. Summary Table

| # | Severity | File | Issue |
|---|----------|------|-------|
| 1 | CRITICAL | `RefreshTokenRepository.java:13` | No `@Lock` on `findByTokenHash` — concurrent refresh races break rotation |
| 2 | HIGH | `AuthService.java:69` | `JwtException` from re-parsing JWT not caught → HTTP 500 |
| 3 | HIGH | `RefreshRequest.java:4` | Missing `@NotBlank` — null token causes NPE → HTTP 500 |
| 4 | HIGH | `V2__refresh_tokens.sql:6` | No UNIQUE constraint or index on `token_hash` |
| 5 | MEDIUM | `AuthService.java:44` | `login()` not `@Transactional` — refresh token can be committed without being delivered |
| 6 | MEDIUM | `AuthService.java:67-71` | JWT `sub` not cross-validated against DB `userId` |
| 7 | MEDIUM | `SecurityConfig.java:65` | `authException.getMessage()` concatenated into JSON without escaping |
| 8 | MEDIUM | `JwtService.java:68-71` | `notExpired` check is dead code — misleads future maintainers |
| 9 | LOW | `GlobalControllerAdvice.java:44` | `AuthenticationException` subtypes return `ProblemDetail` instead of the standard envelope |
| 10 | LOW | `RefreshTokenRevocationService.java` | No comment explaining why this class must remain a separate bean |

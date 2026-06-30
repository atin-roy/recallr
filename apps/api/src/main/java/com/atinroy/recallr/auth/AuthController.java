package com.atinroy.recallr.auth;

import com.atinroy.recallr.auth.dto.AuthResult;
import com.atinroy.recallr.auth.dto.LoginResponse;
import com.atinroy.recallr.auth.dto.LoginRequest;
import com.atinroy.recallr.security.CustomUserDetails;
import com.atinroy.recallr.security.JwtAuthenticationFilter;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.domain.user.dto.UserResponse;
import com.atinroy.recallr.domain.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody EmailRegisterRequest emailRegisterRequest) {
        return authService.register(emailRegisterRequest);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResult result = authService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(result.refreshToken()).toString());
        return new LoginResponse(result.accessToken(), "Bearer", result.user());
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * <p>This endpoint requires a valid Bearer token in the {@code Authorization} header.
     * The {@link JwtAuthenticationFilter} validates the token on every request and, if
     * valid, populates the {@link org.springframework.security.core.context.SecurityContext}
     * with a {@link CustomUserDetails} principal. Spring then injects that principal here
     * via {@link AuthenticationPrincipal} — no additional database call is made.
     *
     * <p>Typical use-cases:
     * <ul>
     *   <li>Frontend bootstrap: fetch the logged-in user's profile after app load.</li>
     *   <li>Token probe: a 200 response confirms the token is still valid; a 401 means
     *       the token has expired or been tampered with.</li>
     * </ul>
     *
     * @param principal the authenticated user injected by Spring Security
     * @return a {@link UserResponse} containing the user's id, email, and roles
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal CustomUserDetails principal) {
        return UserMapper.toResponse(principal);
    }

    /**
     * Issues a new access token and rotates the refresh token.
     * The refresh token is read from the {@code refresh_token} HttpOnly cookie and
     * a new one is written back in the same fashion — the old token is revoked immediately.
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(
            @CookieValue(name = "${spring.application.security.jwt.cookie.name}", required = false)
            String token,
            HttpServletResponse response
    ) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Missing refresh token");
        }
        AuthResult result = authService.refreshToken(token);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.build(result.refreshToken()).toString());
        return new LoginResponse(result.accessToken(), "Bearer", result.user());
    }

    /**
     * Revokes the current refresh token and clears the cookie.
     * Best-effort: returns 204 even if the cookie is absent or the token was already revoked.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(name = "${spring.application.security.jwt.cookie.name}", required = false)
            String token,
            HttpServletResponse response
    ) {
        if (token != null && !token.isBlank()) {
            refreshTokenService.revokeByRawToken(token);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.clear().toString());
    }
}

package com.atinroy.recallr.auth;

import com.atinroy.recallr.auth.dto.AuthResponse;
import com.atinroy.recallr.auth.dto.LoginRequest;
import com.atinroy.recallr.security.CustomUserDetails;
import com.atinroy.recallr.security.JwtAuthenticationFilter;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.user.dto.UserResponse;
import com.atinroy.recallr.user.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody EmailRegisterRequest emailRegisterRequest) {
        return authService.register(emailRegisterRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
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
}

package com.atinroy.recallr.auth;

import com.atinroy.recallr.auth.dto.AuthResult;
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

    @Transactional
    public AuthResult login(LoginRequest request) {
        // Throws AuthenticationException (→ 401) if credentials are wrong
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = refreshTokenService.generateRefreshToken(principal);

        return new AuthResult(accessToken, refreshToken, UserMapper.toResponse(principal));
    }

    @Transactional
    public AuthResult refreshToken(String token) {
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(token);

        UUID uuid = refreshToken.getUserId();
        CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserById(uuid);

        refreshTokenService.revokeToken(refreshToken);
        String newRefreshToken = refreshTokenService.generateRefreshToken(principal);
        String accessToken = jwtService.generateAccessToken(principal);

        return new AuthResult(accessToken, newRefreshToken, UserMapper.toResponse(principal));
    }
}
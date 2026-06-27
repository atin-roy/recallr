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
import org.springframework.security.core.userdetails.UserDetails;
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

    public LoginResponse login(LoginRequest request) {
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

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                UserMapper.toResponse(userRepository.getReferenceById(principal.getId()))
        );
    }

    public LoginResponse refreshToken(String token) {
        if (!refreshTokenService.isTokenValid(token)) {
            throw new InvalidTokenException("Token is not valid");
        }

        String userId = jwtService.extractUsername(token);
        UUID uuid = UUID.fromString(userId);
        CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserById(uuid);
        String accessToken = jwtService.generateAccessToken(principal);

        return new LoginResponse(
                accessToken,
                token,
                "Bearer",
                UserMapper.toResponse(userRepository.getReferenceById(uuid))
        );
    }
}
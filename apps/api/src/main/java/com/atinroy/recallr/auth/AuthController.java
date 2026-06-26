package com.atinroy.recallr.auth;

import com.atinroy.recallr.auth.dto.AuthResponse;
import com.atinroy.recallr.auth.dto.LoginRequest;
import com.atinroy.recallr.user.UserService;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody EmailRegisterRequest emailRegisterRequest) {
        return userService.createUser(emailRegisterRequest);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}

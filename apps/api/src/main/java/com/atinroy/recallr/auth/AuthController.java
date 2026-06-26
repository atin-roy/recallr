package com.atinroy.recallr.auth;

import com.atinroy.recallr.user.UserService;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    @PostMapping("/register")
    public UserResponse register(@RequestBody EmailRegisterRequest emailRegisterRequest) {
        return userService.createUser(emailRegisterRequest);
    }
}

package com.atinroy.recallr.auth.dto;

import com.atinroy.recallr.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user
) {
}
package com.atinroy.recallr.auth.dto;

import com.atinroy.recallr.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
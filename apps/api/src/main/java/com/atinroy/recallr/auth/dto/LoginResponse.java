package com.atinroy.recallr.auth.dto;

import com.atinroy.recallr.domain.user.dto.UserResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
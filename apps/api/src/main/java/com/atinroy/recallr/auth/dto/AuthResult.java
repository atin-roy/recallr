package com.atinroy.recallr.auth.dto;

import com.atinroy.recallr.user.dto.UserResponse;

/**
 * Internal result of a login or token-refresh operation.
 * Contains both the short-lived access token (returned in the JSON body) and
 * the long-lived refresh token (written into an HttpOnly cookie by the controller).
 * Never serialized directly to the HTTP response.
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

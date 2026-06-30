package com.atinroy.recallr.domain.user.dto;

import com.atinroy.recallr.domain.user.Role;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Set;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record UserResponse(
        UUID id,
        String email,
        Set<Role> roles
) {
}

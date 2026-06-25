package com.atinroy.recallr.auth.mapper;

import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.auth.dto.UserResponse;
import com.atinroy.recallr.user.IdentityProvider;
import com.atinroy.recallr.user.User;
import com.atinroy.recallr.user.UserProvider;

import java.util.HashSet;
import java.util.Set;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                Set.copyOf(user.getRoles())
        );
    }

    public static User toEntity(EmailRegisterRequest userRequest, String passwordHash) {
        User user = new User();

        UserProvider userProvider = new UserProvider();
        userProvider.setProvider(IdentityProvider.LOCAL);
        userProvider.setUser(user);
        userProvider.setPasswordHash(passwordHash);
        userProvider.setProviderId(userRequest.email().toLowerCase().strip());

        user.setEmail(userRequest.email());
        user.setProviders(new HashSet<>(Set.of(userProvider)));


        return user;
    }
}

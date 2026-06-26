package com.atinroy.recallr.user.mapper;

import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.user.dto.UserResponse;
import com.atinroy.recallr.user.IdentityProvider;
import com.atinroy.recallr.user.User;
import com.atinroy.recallr.user.UserProvider;

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
        String normalizedEmail = userRequest.email().toLowerCase().strip();

        UserProvider userProvider = new UserProvider();
        userProvider.setProvider(IdentityProvider.LOCAL);
        userProvider.setUser(user);
        userProvider.setPasswordHash(passwordHash);
        userProvider.setProviderId(normalizedEmail);

        user.setEmail(normalizedEmail);
        user.addProvider(userProvider);


        return user;
    }
}

package com.atinroy.recallr.domain.user.mapper;

import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.security.CustomUserDetails;
import com.atinroy.recallr.domain.user.dto.UserResponse;
import com.atinroy.recallr.domain.user.IdentityProvider;
import com.atinroy.recallr.domain.user.Role;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.domain.user.UserProvider;

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

    /**
     * Converts a {@link CustomUserDetails} principal (already resident in the
     * {@link org.springframework.security.core.context.SecurityContext}) into a
     * {@link UserResponse}, avoiding a redundant database round-trip.
     *
     * <p>Roles are reconstructed by stripping the {@code ROLE_} prefix that
     * Spring Security adds when building {@link org.springframework.security.core.GrantedAuthority}
     * objects in {@link CustomUserDetails#from(User, String)}.
     *
     * @param principal the authenticated principal
     * @return the mapped {@link UserResponse}
     */
    public static UserResponse toResponse(CustomUserDetails principal) {
        Set<Role> roles = principal.getAuthorities().stream()
                .map(a -> Role.valueOf(a.getAuthority().replace("ROLE_", "")))
                .collect(java.util.stream.Collectors.toSet());
        return new UserResponse(principal.getId(), principal.getEmail(), roles);
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

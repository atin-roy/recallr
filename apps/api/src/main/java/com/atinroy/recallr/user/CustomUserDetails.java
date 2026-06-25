package com.atinroy.recallr.user;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;


@Getter
public class CustomUserDetails implements UserDetails {
    private final User user;
    private final String password;

    public CustomUserDetails(User user, String password) {
        this.user = user;
        this.password = password;
    }

    public UUID getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public String getUsername() {
        String email = getEmail();
        if (email == null) {
            throw new IllegalStateException("Cannot get username because email is null");
        }
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }
}

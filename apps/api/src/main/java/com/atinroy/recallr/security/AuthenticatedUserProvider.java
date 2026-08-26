package com.atinroy.recallr.security;

import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.domain.user.UserNotFoundException;
import com.atinroy.recallr.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserNotFoundException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new UserNotFoundException("Invalid authenticated user");
        }

        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }
}

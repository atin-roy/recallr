package com.atinroy.recallr.security;

import com.atinroy.recallr.domain.user.IdentityProvider;
import com.atinroy.recallr.domain.user.User;
import com.atinroy.recallr.domain.user.UserProvider;
import com.atinroy.recallr.domain.user.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase().strip()).orElseThrow(() -> new UsernameNotFoundException(email));
        String password = user.getProviders()
                .stream()
                .filter(p -> p.getProvider() == IdentityProvider.LOCAL)
                .findFirst()
                .map(UserProvider::getPasswordHash)
                .orElseThrow(() -> new UsernameNotFoundException("No local login for this account"));
        return CustomUserDetails.from(user, password);
    }

    /**
     * Loads a user by their internal UUID. Used by the JWT authentication filter
     * where the token {@code sub} claim is the user's UUID, not their email.
     *
     * <p>This keeps the JWT validation path decoupled from email, which allows
     * OAuth users without an email address to be authenticated correctly.
     *
     * @param id the user's internal UUID
     * @return the populated {@link UserDetails} principal
     * @throws UsernameNotFoundException if no user exists with the given id
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(@NonNull UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));
        String password = user.getProviders().stream()
                .filter(p -> p.getProvider() == IdentityProvider.LOCAL)
                .findFirst()
                .map(UserProvider::getPasswordHash)
                .orElse(null); // OAuth-only users have no password hash
        return CustomUserDetails.from(user, password);
    }
}

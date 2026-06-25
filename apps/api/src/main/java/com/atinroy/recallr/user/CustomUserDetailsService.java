package com.atinroy.recallr.user;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        String password = user.getProviders()
                .stream()
                .filter(p -> p.getProvider() == IdentityProvider.LOCAL)
                .findFirst()
                .map(UserProvider::getPasswordHash)
                .orElseThrow(() -> new UsernameNotFoundException("No local login for this account"));
        return CustomUserDetails.from(user, password);
    }
}

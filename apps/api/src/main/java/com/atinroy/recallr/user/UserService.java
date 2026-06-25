package com.atinroy.recallr.user;

import com.atinroy.recallr.auth.mapper.UserMapper;
import com.atinroy.recallr.auth.config.PasswordEncoder;
import com.atinroy.recallr.auth.dto.EmailRegisterRequest;
import com.atinroy.recallr.auth.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(EmailRegisterRequest userRequest) {
        String passwordHash = passwordEncoder.encode(userRequest.password());
        User user = UserMapper.toEntity(userRequest, passwordHash);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }
}

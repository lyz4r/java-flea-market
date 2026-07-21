package com.flea.market.user;

import com.flea.market.common.ConflictException;
import com.flea.market.common.NotFoundException;
import com.flea.market.user.dto.RegisterRequest;
import com.flea.market.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new ConflictException("Login is already taken: " + request.login());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered: " + request.email());
        }

        User user = User.builder()
                .login(request.login())
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .blocked(false)
                .build();

        return userMapper.toResponse(userRepository.save(user));
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(findById(id));
    }

    public UserResponse getCurrent(String login) {
        return userMapper.toResponse(userRepository.findByLogin(login)
                .orElseThrow(() -> new NotFoundException("User not found: " + login)));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Transactional
    public UserResponse block(Long id) {
        User user = findById(id);
        if (user.getRole() == Role.ADMIN) {
            throw new ConflictException("Admin users cannot be blocked");
        }
        user.setBlocked(true);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public UserResponse unblock(Long id) {
        User user = findById(id);
        user.setBlocked(false);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }
}

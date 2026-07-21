package com.flea.market.user;

import com.flea.market.common.ConflictException;
import com.flea.market.common.NotFoundException;
import com.flea.market.user.dto.RegisterRequest;
import com.flea.market.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private RegisterRequest request() {
        return new RegisterRequest("john", "John Doe", "john@example.com", "secret123");
    }

    @Test
    void register_success() {
        RegisterRequest request = request();
        when(userRepository.existsByLogin(request.login())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-hash");

        User saved = User.builder()
                .id(1L)
                .login(request.login())
                .name(request.name())
                .email(request.email())
                .passwordHash("bcrypt-hash")
                .role(Role.USER)
                .blocked(false)
                .createdAt(LocalDateTime.now())
                .build();
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse expected = new UserResponse(1L, "john", "John Doe", "john@example.com",
                Role.USER, false, saved.getCreatedAt());
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse result = userService.register(request);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User toSave = captor.getValue();
        assertThat(toSave.getRole()).isEqualTo(Role.USER);
        assertThat(toSave.isBlocked()).isFalse();
        assertThat(toSave.getPasswordHash()).isEqualTo("bcrypt-hash");
    }

    @Test
    void register_duplicateLogin_throwsConflict() {
        RegisterRequest request = request();
        when(userRepository.existsByLogin(request.login())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Login");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = request();
        when(userRepository.existsByLogin(request.login())).thenReturn(false);
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(42L))
                .isInstanceOf(NotFoundException.class);
    }
}

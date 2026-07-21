package com.flea.market.user;

import com.flea.market.common.ConflictException;
import com.flea.market.config.SecurityConfig;
import com.flea.market.security.CustomUserDetailsService;
import com.flea.market.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private void stubAuth(String login, String role) {
        UserDetails userDetails = User.withUsername(login)
                .password(new BCryptPasswordEncoder().encode("secret123"))
                .roles(role)
                .build();
        when(userDetailsService.loadUserByUsername(login)).thenReturn(userDetails);
    }

    private UserResponse blockedUser() {
        return new UserResponse(2L, "ivan", "Ivan", "ivan@example.com",
                Role.USER, true, LocalDateTime.now());
    }

    @Test
    void block_asAdmin_returnsOk() throws Exception {
        stubAuth("admin", "ADMIN");
        when(userService.block(2L)).thenReturn(blockedUser());

        mockMvc.perform(patch("/api/users/2/block")
                        .with(httpBasic("admin", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));
    }

    @Test
    void unblock_asAdmin_returnsOk() throws Exception {
        stubAuth("admin", "ADMIN");
        when(userService.unblock(2L)).thenReturn(
                new UserResponse(2L, "ivan", "Ivan", "ivan@example.com",
                        Role.USER, false, LocalDateTime.now()));

        mockMvc.perform(patch("/api/users/2/unblock")
                        .with(httpBasic("admin", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));
    }

    @Test
    void block_asRegularUser_returnsForbidden() throws Exception {
        stubAuth("ivan", "USER");

        mockMvc.perform(patch("/api/users/2/block")
                        .with(httpBasic("ivan", "secret123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void block_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/users/2/block"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void block_adminTarget_returnsConflict() throws Exception {
        stubAuth("admin", "ADMIN");
        when(userService.block(1L)).thenThrow(new ConflictException("Admin users cannot be blocked"));

        mockMvc.perform(patch("/api/users/1/block")
                        .with(httpBasic("admin", "secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Admin users cannot be blocked"));
    }
}

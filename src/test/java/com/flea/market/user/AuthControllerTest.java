package com.flea.market.user;

import com.flea.market.common.ConflictException;
import com.flea.market.config.SecurityConfig;
import com.flea.market.security.CustomUserDetailsService;
import com.flea.market.user.dto.RegisterRequest;
import com.flea.market.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private RegisterRequest validRequest() {
        return new RegisterRequest("john", "John Doe", "john@example.com", "secret123");
    }

    @Test
    void register_valid_returnsCreated() throws Exception {
        UserResponse response = new UserResponse(1L, "john", "John Doe", "john@example.com",
                Role.USER, false, LocalDateTime.now());
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("john"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_invalid_returnsBadRequest() throws Exception {
        RegisterRequest invalid = new RegisterRequest("jo", "", "not-an-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.login").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void register_duplicate_returnsConflict() throws Exception {
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Login is already taken: john"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Login is already taken: john"));
    }

    @Test
    void me_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_returnsProfile() throws Exception {
        UserResponse response = new UserResponse(1L, "john", "John Doe", "john@example.com",
                Role.USER, false, LocalDateTime.now());
        UserDetails userDetails = User.withUsername("john")
                .password(new BCryptPasswordEncoder().encode("secret123"))
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("john")).thenReturn(userDetails);
        when(userService.getCurrent("john")).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("john", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("john"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }
}

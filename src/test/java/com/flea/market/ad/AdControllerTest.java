package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import com.flea.market.common.ForbiddenException;
import com.flea.market.config.SecurityConfig;
import com.flea.market.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdController.class)
@Import(SecurityConfig.class)
class AdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdService adService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private void stubAuth() {
        UserDetails userDetails = User.withUsername("ivan")
                .password(new BCryptPasswordEncoder().encode("secret123"))
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("ivan")).thenReturn(userDetails);
    }

    private AdRequest validRequest() {
        return new AdRequest("Bike", "Almost new", "Transport", new BigDecimal("15000.00"), null);
    }

    private AdResponse response() {
        return new AdResponse(10L, 1L, "ivan", "Bike", "Almost new", "Transport",
                new BigDecimal("15000.00"), true, null, AdStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void create_valid_returnsCreated() throws Exception {
        stubAuth();
        when(adService.create(any(AdRequest.class), eq("ivan"))).thenReturn(response());

        mockMvc.perform(post("/api/ads")
                        .with(httpBasic("ivan", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Bike"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_invalid_returnsBadRequest() throws Exception {
        stubAuth();
        AdRequest invalid = new AdRequest("", null, "", new BigDecimal("-1"), null);

        mockMvc.perform(post("/api/ads")
                        .with(httpBasic("ivan", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.category").exists())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void create_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_valid_returnsOk() throws Exception {
        stubAuth();
        when(adService.update(eq(10L), any(AdRequest.class), eq("ivan"))).thenReturn(response());

        mockMvc.perform(put("/api/ads/10")
                        .with(httpBasic("ivan", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void update_notAuthor_returnsForbidden() throws Exception {
        stubAuth();
        when(adService.update(eq(10L), any(AdRequest.class), eq("ivan")))
                .thenThrow(new ForbiddenException("Only the author can edit the ad"));

        mockMvc.perform(put("/api/ads/10")
                        .with(httpBasic("ivan", "secret123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the author can edit the ad"));
    }
}

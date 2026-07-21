package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import com.flea.market.common.ForbiddenException;
import com.flea.market.common.NotFoundException;
import com.flea.market.config.SecurityConfig;
import com.flea.market.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void search_returnsPage() throws Exception {
        stubAuth();
        Page<AdResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1);
        when(adService.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/ads")
                        .with(httpBasic("ivan", "secret123"))
                        .param("title", "Bike")
                        .param("category", "Transport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Bike"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void search_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/ads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyAds_returnsPage() throws Exception {
        stubAuth();
        Page<AdResponse> page = new PageImpl<>(List.of(response()), PageRequest.of(0, 10), 1);
        when(adService.getMyAds(eq("ivan"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/ads/my")
                        .with(httpBasic("ivan", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorLogin").value("ivan"));
    }

    @Test
    void getById_returnsAd() throws Exception {
        stubAuth();
        when(adService.getById(10L, "ivan")).thenReturn(response());

        mockMvc.perform(get("/api/ads/10")
                        .with(httpBasic("ivan", "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void getById_notFound_returnsNotFound() throws Exception {
        stubAuth();
        when(adService.getById(999L, "ivan"))
                .thenThrow(new NotFoundException("Ad not found: 999"));

        mockMvc.perform(get("/api/ads/999")
                        .with(httpBasic("ivan", "secret123")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ad not found: 999"));
    }
}

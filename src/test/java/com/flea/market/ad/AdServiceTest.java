package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import com.flea.market.common.BadRequestException;
import com.flea.market.common.ConflictException;
import com.flea.market.common.ForbiddenException;
import com.flea.market.common.NotFoundException;
import com.flea.market.user.Role;
import com.flea.market.user.User;
import com.flea.market.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdServiceTest {

    @Mock
    private AdRepository adRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdMapper adMapper;

    @InjectMocks
    private AdService adService;

    private User author() {
        return User.builder()
                .id(1L)
                .login("ivan")
                .name("Ivan")
                .email("ivan@example.com")
                .passwordHash("hash")
                .role(Role.USER)
                .blocked(false)
                .build();
    }

    private AdRequest numericPriceRequest() {
        return new AdRequest("Bike", "Almost new", "Transport",
                new BigDecimal("15000.00"), null);
    }

    private Ad existingAd(User author) {
        return Ad.builder()
                .id(10L)
                .author(author)
                .title("Old title")
                .description("Old description")
                .category("Old category")
                .price(new BigDecimal("100.00"))
                .priceIsNumeric(true)
                .status(AdStatus.ACTIVE)
                .adminDeactivated(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    @Test
    void create_success() {
        User author = author();
        AdRequest request = numericPriceRequest();
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adMapper.toResponse(any(Ad.class))).thenReturn(
                new AdResponse(10L, 1L, "ivan", request.title(), request.description(),
                        request.category(), request.price(), true, null,
                        AdStatus.ACTIVE, false, LocalDateTime.now(), LocalDateTime.now()));

        AdResponse result = adService.create(request, "ivan");

        assertThat(result.status()).isEqualTo(AdStatus.ACTIVE);

        ArgumentCaptor<Ad> captor = ArgumentCaptor.forClass(Ad.class);
        verify(adRepository).save(captor.capture());
        Ad toSave = captor.getValue();
        assertThat(toSave.getAuthor()).isEqualTo(author);
        assertThat(toSave.getStatus()).isEqualTo(AdStatus.ACTIVE);
        assertThat(toSave.isAdminDeactivated()).isFalse();
        assertThat(toSave.isPriceIsNumeric()).isTrue();
        assertThat(toSave.getPriceText()).isNull();
    }

    @Test
    void create_textPrice_success() {
        User author = author();
        AdRequest request = new AdRequest("Bike", "Almost new", "Transport", null, "договорная");
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));
        when(adRepository.save(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adMapper.toResponse(any(Ad.class))).thenReturn(
                new AdResponse(10L, 1L, "ivan", request.title(), request.description(),
                        request.category(), null, false, "договорная",
                        AdStatus.ACTIVE, false, LocalDateTime.now(), LocalDateTime.now()));

        AdResponse result = adService.create(request, "ivan");

        assertThat(result.priceIsNumeric()).isFalse();
        assertThat(result.priceText()).isEqualTo("договорная");
    }

    @Test
    void create_bothPrices_throwsBadRequest() {
        AdRequest request = new AdRequest("Bike", "d", "Transport",
                new BigDecimal("100"), "договорная");

        assertThatThrownBy(() -> adService.create(request, "ivan"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void create_noPrice_throwsBadRequest() {
        AdRequest request = new AdRequest("Bike", "d", "Transport", null, null);

        assertThatThrownBy(() -> adService.create(request, "ivan"))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(userRepository);
    }

    @Test
    void create_blockedUser_throwsForbidden() {
        User author = author();
        author.setBlocked(true);
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> adService.create(numericPriceRequest(), "ivan"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_success() {
        User author = author();
        Ad ad = existingAd(author);
        AdRequest request = numericPriceRequest();
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(adRepository.saveAndFlush(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adMapper.toResponse(any(Ad.class))).thenReturn(
                new AdResponse(10L, 1L, "ivan", request.title(), request.description(),
                        request.category(), request.price(), true, null,
                        AdStatus.ACTIVE, false, ad.getCreatedAt(), LocalDateTime.now()));

        AdResponse result = adService.update(10L, request, "ivan");

        assertThat(result.title()).isEqualTo("Bike");
        assertThat(ad.getTitle()).isEqualTo("Bike");
        assertThat(ad.getCategory()).isEqualTo("Transport");
        assertThat(ad.getStatus()).isEqualTo(AdStatus.ACTIVE);
    }

    @Test
    void update_notAuthor_throwsForbidden() {
        Ad ad = existingAd(author());
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.update(10L, numericPriceRequest(), "someone"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_blockedAuthor_throwsForbidden() {
        User author = author();
        author.setBlocked(true);
        Ad ad = existingAd(author);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.update(10L, numericPriceRequest(), "ivan"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_notFound_throwsNotFound() {
        when(adRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adService.update(42L, numericPriceRequest(), "ivan"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void search_minPriceGreaterThanMaxPrice_throwsBadRequest() {
        assertThatThrownBy(() -> adService.search(null, null,
                new BigDecimal("200"), new BigDecimal("100"), null,
                PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_returnsMappedPage() {
        Ad ad = existingAd(author());
        Page<Ad> page = new PageImpl<>(List.of(ad), PageRequest.of(0, 10), 1);
        when(adRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        AdResponse response = new AdResponse(10L, 1L, "ivan", "Old title", "Old description",
                "Old category", new BigDecimal("100.00"), true, null, AdStatus.ACTIVE, false,
                ad.getCreatedAt(), ad.getCreatedAt());
        when(adMapper.toResponse(ad)).thenReturn(response);

        Page<AdResponse> result = adService.search("old", "Old category",
                new BigDecimal("50"), new BigDecimal("150"), "ivan", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getMyAds_returnsMappedPage() {
        Ad ad = existingAd(author());
        ad.setStatus(AdStatus.INACTIVE);
        Page<Ad> page = new PageImpl<>(List.of(ad), PageRequest.of(0, 10), 1);
        when(adRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        AdResponse response = new AdResponse(10L, 1L, "ivan", "Old title", "Old description",
                "Old category", new BigDecimal("100.00"), true, null, AdStatus.INACTIVE, false,
                ad.getCreatedAt(), ad.getCreatedAt());
        when(adMapper.toResponse(ad)).thenReturn(response);

        Page<AdResponse> result = adService.getMyAds("ivan", PageRequest.of(0, 10));

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void getById_activeAd_visibleToAnyone() {
        Ad ad = existingAd(author());
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(adMapper.toResponse(ad)).thenReturn(
                new AdResponse(10L, 1L, "ivan", "Old title", "Old description", "Old category",
                        new BigDecimal("100.00"), true, null, AdStatus.ACTIVE, false,
                        ad.getCreatedAt(), ad.getCreatedAt()));

        AdResponse result = adService.getById(10L, "someone");

        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    void getById_inactiveAd_hiddenFromOthers() {
        Ad ad = existingAd(author());
        ad.setStatus(AdStatus.INACTIVE);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));

        assertThatThrownBy(() -> adService.getById(10L, "someone"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_inactiveAd_visibleToAuthor() {
        Ad ad = existingAd(author());
        ad.setStatus(AdStatus.INACTIVE);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(adMapper.toResponse(ad)).thenReturn(
                new AdResponse(10L, 1L, "ivan", "Old title", "Old description", "Old category",
                        new BigDecimal("100.00"), true, null, AdStatus.INACTIVE, false,
                        ad.getCreatedAt(), ad.getCreatedAt()));

        AdResponse result = adService.getById(10L, "ivan");

        assertThat(result.status()).isEqualTo(AdStatus.INACTIVE);
    }

    private User admin() {
        return User.builder()
                .id(99L)
                .login("admin")
                .name("Admin")
                .email("admin@flea.market")
                .passwordHash("hash")
                .role(Role.ADMIN)
                .blocked(false)
                .build();
    }

    private void stubAdAndRequester(Ad ad, User requester) {
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin(requester.getLogin())).thenReturn(Optional.of(requester));
        when(adRepository.saveAndFlush(any(Ad.class))).thenAnswer(inv -> inv.getArgument(0));
        when(adMapper.toResponse(any(Ad.class))).thenAnswer(inv -> {
            Ad saved = inv.getArgument(0);
            return new AdResponse(saved.getId(), 1L, "ivan", saved.getTitle(),
                    saved.getDescription(), saved.getCategory(), saved.getPrice(),
                    saved.isPriceIsNumeric(), saved.getPriceText(), saved.getStatus(),
                    saved.isAdminDeactivated(), saved.getCreatedAt(), saved.getCreatedAt());
        });
    }

    @Test
    void deactivate_byAuthor_setsInactive_keepsFlag() {
        User author = author();
        Ad ad = existingAd(author);
        stubAdAndRequester(ad, author);

        AdResponse result = adService.deactivate(10L, "ivan");

        assertThat(result.status()).isEqualTo(AdStatus.INACTIVE);
        assertThat(result.adminDeactivated()).isFalse();
    }

    @Test
    void deactivate_byBlockedAuthor_throwsForbidden() {
        User author = author();
        author.setBlocked(true);
        Ad ad = existingAd(author);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> adService.deactivate(10L, "ivan"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivate_byAdmin_setsInactiveAndFlag() {
        Ad ad = existingAd(author());
        stubAdAndRequester(ad, admin());

        AdResponse result = adService.deactivate(10L, "admin");

        assertThat(result.status()).isEqualTo(AdStatus.INACTIVE);
        assertThat(result.adminDeactivated()).isTrue();
    }

    @Test
    void deactivate_byOtherUser_throwsForbidden() {
        Ad ad = existingAd(author());
        User other = author();
        other.setLogin("someone");
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin("someone")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> adService.deactivate(10L, "someone"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void activate_byAuthor_setsActive() {
        User author = author();
        Ad ad = existingAd(author);
        ad.setStatus(AdStatus.INACTIVE);
        stubAdAndRequester(ad, author);

        AdResponse result = adService.activate(10L, "ivan");

        assertThat(result.status()).isEqualTo(AdStatus.ACTIVE);
    }

    @Test
    void activate_byAuthor_adminDeactivated_throwsConflict() {
        User author = author();
        Ad ad = existingAd(author);
        ad.setStatus(AdStatus.INACTIVE);
        ad.setAdminDeactivated(true);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> adService.activate(10L, "ivan"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("administrator");
    }

    @Test
    void activate_byBlockedAuthor_throwsForbidden() {
        User author = author();
        author.setBlocked(true);
        Ad ad = existingAd(author);
        ad.setStatus(AdStatus.INACTIVE);
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin("ivan")).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> adService.activate(10L, "ivan"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void activate_byAdmin_setsActive_clearsFlag() {
        Ad ad = existingAd(author());
        ad.setStatus(AdStatus.INACTIVE);
        ad.setAdminDeactivated(true);
        stubAdAndRequester(ad, admin());

        AdResponse result = adService.activate(10L, "admin");

        assertThat(result.status()).isEqualTo(AdStatus.ACTIVE);
        assertThat(result.adminDeactivated()).isFalse();
    }

    @Test
    void activate_byOtherUser_throwsForbidden() {
        Ad ad = existingAd(author());
        ad.setStatus(AdStatus.INACTIVE);
        User other = author();
        other.setLogin("someone");
        when(adRepository.findById(10L)).thenReturn(Optional.of(ad));
        when(userRepository.findByLogin("someone")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> adService.activate(10L, "someone"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void activate_alreadyActive_idempotent() {
        User author = author();
        Ad ad = existingAd(author);
        stubAdAndRequester(ad, author);

        AdResponse result = adService.activate(10L, "ivan");

        assertThat(result.status()).isEqualTo(AdStatus.ACTIVE);
    }
}

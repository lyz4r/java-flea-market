package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import com.flea.market.common.BadRequestException;
import com.flea.market.common.ForbiddenException;
import com.flea.market.common.NotFoundException;
import com.flea.market.user.User;
import com.flea.market.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdService {

    private final AdRepository adRepository;
    private final UserRepository userRepository;
    private final AdMapper adMapper;

    @Transactional
    public AdResponse create(AdRequest request, String authorLogin) {
        validatePrice(request);

        User author = userRepository.findByLogin(authorLogin)
                .orElseThrow(() -> new NotFoundException("User not found: " + authorLogin));
        if (author.isBlocked()) {
            throw new ForbiddenException("Blocked users cannot create ads");
        }

        Ad ad = Ad.builder()
                .author(author)
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .price(request.price())
                .priceIsNumeric(request.price() != null)
                .priceText(request.price() != null ? null : request.priceText())
                .status(AdStatus.ACTIVE)
                .adminDeactivated(false)
                .build();

        return adMapper.toResponse(adRepository.save(ad));
    }

    @Transactional
    public AdResponse update(Long id, AdRequest request, String login) {
        validatePrice(request);

        Ad ad = findById(id);
        if (!ad.getAuthor().getLogin().equals(login)) {
            throw new ForbiddenException("Only the author can edit the ad");
        }
        if (ad.getAuthor().isBlocked()) {
            throw new ForbiddenException("Blocked users cannot edit ads");
        }

        ad.setTitle(request.title());
        ad.setDescription(request.description());
        ad.setCategory(request.category());
        ad.setPrice(request.price());
        ad.setPriceIsNumeric(request.price() != null);
        ad.setPriceText(request.price() != null ? null : request.priceText());

        return adMapper.toResponse(adRepository.saveAndFlush(ad));
    }

    public Ad findById(Long id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ad not found: " + id));
    }

    private void validatePrice(AdRequest request) {
        boolean hasNumericPrice = request.price() != null;
        boolean hasPriceText = request.priceText() != null && !request.priceText().isBlank();
        if (hasNumericPrice == hasPriceText) {
            throw new BadRequestException("Exactly one of price or priceText must be provided");
        }
    }
}

package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private static final int MAX_PAGE_SIZE = 50;

    private final AdService adService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdResponse create(@Valid @RequestBody AdRequest request, Principal principal) {
        return adService.create(request, principal.getName());
    }

    @PutMapping("/{id}")
    public AdResponse update(@PathVariable Long id,
                             @Valid @RequestBody AdRequest request,
                             Principal principal) {
        return adService.update(id, request, principal.getName());
    }

    @GetMapping
    public Page<AdResponse> search(@RequestParam(required = false) String title,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) BigDecimal minPrice,
                                   @RequestParam(required = false) BigDecimal maxPrice,
                                   @RequestParam(required = false) String author,
                                   @PageableDefault(size = 10, sort = "createdAt",
                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return adService.search(title, category, minPrice, maxPrice, author, capPageSize(pageable));
    }

    @GetMapping("/my")
    public Page<AdResponse> getMyAds(@PageableDefault(size = 10, sort = "createdAt",
                                             direction = Sort.Direction.DESC) Pageable pageable,
                                     Principal principal) {
        return adService.getMyAds(principal.getName(), capPageSize(pageable));
    }

    @GetMapping("/{id}")
    public AdResponse getById(@PathVariable Long id, Principal principal) {
        return adService.getById(id, principal.getName());
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            return PageRequest.of(pageable.getPageNumber(), MAX_PAGE_SIZE, pageable.getSort());
        }
        return pageable;
    }
}

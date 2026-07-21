package com.flea.market.ad;

import com.flea.market.ad.dto.AdRequest;
import com.flea.market.ad.dto.AdResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

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
}

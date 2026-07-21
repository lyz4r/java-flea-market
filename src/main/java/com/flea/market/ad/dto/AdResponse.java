package com.flea.market.ad.dto;

import com.flea.market.ad.AdStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdResponse(
        Long id,
        Long authorId,
        String authorLogin,
        String title,
        String description,
        String category,
        BigDecimal price,
        boolean priceIsNumeric,
        String priceText,
        AdStatus status,
        boolean adminDeactivated,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

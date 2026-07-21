package com.flea.market.ad.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        @NotBlank(message = "Category is required")
        @Size(max = 128, message = "Category must be at most 128 characters")
        String category,

        @PositiveOrZero(message = "Price must be zero or positive")
        @Digits(integer = 17, fraction = 2, message = "Price must have at most 17 integer and 2 fraction digits")
        BigDecimal price,

        @Size(max = 255, message = "Price text must be at most 255 characters")
        String priceText
) {
}

package com.flea.market.user.dto;

import com.flea.market.user.Role;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String login,
        String name,
        String email,
        Role role,
        boolean blocked,
        LocalDateTime createdAt
) {
}

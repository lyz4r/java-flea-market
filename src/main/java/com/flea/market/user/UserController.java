package com.flea.market.user;

import com.flea.market.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PatchMapping("/{id}/block")
    public UserResponse block(@PathVariable Long id) {
        return userService.block(id);
    }

    @PatchMapping("/{id}/unblock")
    public UserResponse unblock(@PathVariable Long id) {
        return userService.unblock(id);
    }
}

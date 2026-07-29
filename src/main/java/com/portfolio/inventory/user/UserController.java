package com.portfolio.inventory.user;

import static com.portfolio.inventory.user.UserDtos.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "View the authenticated user's profile")
    public UserResponse current(Authentication authentication) {
        return userService.current(authentication);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "ADMIN only")
    public Page<UserResponse> list(
            @ParameterObject @PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return userService.list(pageable);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a user's role", description = "ADMIN only")
    public UserResponse changeRole(
            @PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        return userService.changeRole(id, request);
    }
}

package com.portfolio.inventory.user;

import static com.portfolio.inventory.user.UserDtos.*;

import com.portfolio.inventory.common.NotFoundException;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse current(Authentication authentication) {
        return UserResponse.from(findByEmail(authentication.getName()));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }

    @Transactional
    public UserResponse changeRole(Long id, ChangeRoleRequest request) {
        UserAccount user =
                users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        user.changeRole(request.role());
        return UserResponse.from(user);
    }

    private UserAccount findByEmail(String email) {
        return users.findByEmailIgnoreCase(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}

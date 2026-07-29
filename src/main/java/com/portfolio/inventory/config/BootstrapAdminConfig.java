package com.portfolio.inventory.config;

import com.portfolio.inventory.user.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminConfig implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String displayName;

    public BootstrapAdminConfig(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password,
            @Value("${app.bootstrap-admin.display-name:System Administrator}")
                    String displayName) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Bootstrap administrator password must contain at least 8 characters");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!users.existsByEmailIgnoreCase(normalizedEmail)) {
            users.save(
                    new UserAccount(
                            normalizedEmail,
                            passwordEncoder.encode(password),
                            displayName.trim(),
                            Role.ADMIN));
        }
    }
}

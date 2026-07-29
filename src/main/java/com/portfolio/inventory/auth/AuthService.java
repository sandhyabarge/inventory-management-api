package com.portfolio.inventory.auth;

import static com.portfolio.inventory.user.UserDtos.*;

import com.portfolio.inventory.common.ConflictException;
import com.portfolio.inventory.user.*;
import java.util.Locale;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwt) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        UserAccount user =
                users.save(
                        new UserAccount(
                                email,
                                passwordEncoder.encode(request.password()),
                                request.displayName().trim(),
                                Role.VIEWER));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        UserAccount user = users.findByEmailIgnoreCase(email).orElseThrow();
        return response(user);
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(
                jwt.create(user), "Bearer", jwt.expirationMs(), UserResponse.from(user));
    }
}

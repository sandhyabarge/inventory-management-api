package com.portfolio.inventory.config;

import com.portfolio.inventory.auth.JwtAuthenticationFilter;
import com.portfolio.inventory.user.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return email ->
                users.findByEmailIgnoreCase(email)
                        .map(
                                user ->
                                        User.withUsername(user.getEmail())
                                                .password(user.getPasswordHash())
                                                .roles(user.getRole().name())
                                                .disabled(!user.isActive())
                                                .build())
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt)
            throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        sessions ->
                                sessions.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        requests ->
                                requests.requestMatchers(
                                                "/api/auth/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/actuator/health")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exceptions ->
                                exceptions.authenticationEntryPoint(
                                                (request, response, exception) -> {
                                                    response.setStatus(
                                                            HttpServletResponse.SC_UNAUTHORIZED);
                                                    response.setContentType(
                                                            "application/problem+json");
                                                    response.getWriter()
                                                            .write(
                                                                    "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"A valid bearer token is required\"}");
                                                })
                                        .accessDeniedHandler(
                                                (request, response, exception) -> {
                                                    response.setStatus(
                                                            HttpServletResponse.SC_FORBIDDEN);
                                                    response.setContentType(
                                                            "application/problem+json");
                                                    response.getWriter()
                                                            .write(
                                                                    "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"You do not have permission for this action\"}");
                                                }))
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

package com.portfolio.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio.inventory.auth.JwtService;
import com.portfolio.inventory.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthUserIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwt;

    @BeforeEach
    void clean() {
        users.deleteAll();
    }

    @Test
    void registerLoginAndReadCurrentProfile() throws Exception {
        String registration =
                """
                {
                  "email": "viewer@example.com",
                  "password": "password123",
                  "displayName": "Demo Viewer"
                }
                """;

        String response =
                mvc.perform(
                                post("/api/auth/register")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(registration))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.token").isNotEmpty())
                        .andExpect(jsonPath("$.user.role").value("VIEWER"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode registered = objectMapper.readTree(response);
        String token = registered.path("token").asText();

        mvc.perform(
                        get("/api/users/me")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("viewer@example.com"))
                .andExpect(jsonPath("$.displayName").value("Demo Viewer"));

        mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "viewer@example.com",
                                          "password": "password123"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void adminCanListUsersAndChangeRole() throws Exception {
        UserAccount admin =
                users.save(
                        new UserAccount(
                                "admin@example.com",
                                passwordEncoder.encode("admin12345"),
                                "Administrator",
                                Role.ADMIN));
        UserAccount viewer =
                users.save(
                        new UserAccount(
                                "viewer@example.com",
                                passwordEncoder.encode("password123"),
                                "Viewer",
                                Role.VIEWER));
        String adminToken = jwt.create(admin);

        mvc.perform(
                        get("/api/users")
                                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mvc.perform(
                        patch("/api/users/{id}/role", viewer.getId())
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"role\":\"INVENTORY_MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INVENTORY_MANAGER"));
    }

    @Test
    void viewerCannotUseAdminUserEndpoints() throws Exception {
        UserAccount viewer =
                users.save(
                        new UserAccount(
                                "viewer@example.com",
                                passwordEncoder.encode("password123"),
                                "Viewer",
                                Role.VIEWER));

        mvc.perform(
                        get("/api/users")
                                .header("Authorization", "Bearer " + jwt.create(viewer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateRegistrationAndInvalidPayloadReturnProblemDetails() throws Exception {
        users.save(
                new UserAccount(
                        "existing@example.com",
                        passwordEncoder.encode("password123"),
                        "Existing",
                        Role.VIEWER));

        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "existing@example.com",
                                          "password": "password123",
                                          "displayName": "Duplicate"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType.APPLICATION_PROBLEM_JSON));

        mvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "invalid",
                                          "password": "short",
                                          "displayName": ""
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }
}

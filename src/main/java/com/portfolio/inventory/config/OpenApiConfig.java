package com.portfolio.inventory.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI inventoryOpenApi() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Inventory Management API")
                                .version("0.1.0")
                                .description(
                                        "User and role management milestone for a multi-warehouse inventory platform."))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        scheme,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme));
    }
}

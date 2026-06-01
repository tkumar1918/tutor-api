package dev.tushar.tutorapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the OpenAPI metadata exposed at {@code /v3/api-docs} and rendered by Swagger UI
 * at {@code /swagger-ui.html}. The {@code bearerAuth} security scheme registered here is
 * referenced from controllers via {@code @SecurityRequirement(name = "bearerAuth")},
 * giving Swagger UI an "Authorize" button.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI tutorApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tutor API")
                        .description(
                                "Beginner-friendly Spring Boot tutorial REST API showcasing DTOs, "
                                        + "validation, mapping, pagination, JWT auth, and global exception handling.")
                        .version("v1")
                        .contact(new Contact().name("Tushar").email("dev@tushar.dev"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the JWT returned from /api/v1/auth/login")));
    }
}

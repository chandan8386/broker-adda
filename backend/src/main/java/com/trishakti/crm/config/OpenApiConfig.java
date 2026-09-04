package com.trishakti.crm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI crmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shri Trishakti Infra Realtors Pvt Ltd — Property CRM API")
                        .description("REST API for Property Lead Management / CRM. Shared by the web and mobile apps.")
                        .version("1.0.0")
                        .contact(new Contact().name("Shri Trishakti Infra Realtors Pvt Ltd").email("tech@trishakti.com"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the accessToken returned by POST /auth/login")));
    }
}

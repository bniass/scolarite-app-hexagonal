package com.ecole221.anneeacademique.service.infrastructure.web.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.keycloak.public-url}")
    private String keycloakPublicUrl;

    @Value("${app.keycloak.realm}")
    private String realm;

    @Bean
    public OpenAPI openAPI() {
        String tokenUrl = keycloakPublicUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return new OpenAPI()
                .info(new Info().title("Annee Academique Service API").version("v1"))
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .password(new OAuthFlow().tokenUrl(tokenUrl)))));
    }
}

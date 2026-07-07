package com.sistema.transporte.reserva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentacion OpenAPI (Swagger UI) con esquema Bearer JWT:
 * el boton "Authorize" de Swagger permite pegar el token
 * (obtenido en /dev/token en el perfil local, o de Keycloak en prod)
 * y probar todos los endpoints desde el navegador.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_BEARER = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ms-reserva | API de Reservas")
                        .description("Microservicio de reservas del Sistema de Transporte. "
                                + "Todos los endpoints requieren JWT (RBAC USER/ADMIN). "
                                + "En perfil local, obtener token en POST /dev/token.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_BEARER))
                .components(new Components().addSecuritySchemes(SCHEME_BEARER,
                        new SecurityScheme()
                                .name(SCHEME_BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}

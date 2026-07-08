package com.sistema.transporte.reserva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import feign.RequestInterceptor;

/**
 * Propaga el JWT del usuario autenticado en las llamadas Feign a otros
 * microservicios (ms-pagos).
 *
 * Antes, ms-pagos no exigia autenticacion, asi que la llamada interna viajaba
 * "a pelo". Ahora ms-pagos es un Resource Server y rechaza peticiones sin
 * token; este interceptor reenvia el mismo Bearer que trae la peticion del
 * usuario, manteniendo la identidad extremo a extremo (defensa en profundidad).
 *
 * Si no hay contexto de seguridad (p. ej. un job interno), no añade cabecera:
 * la llamada seguira sin token y ms-pagos respondera 401, que es lo correcto.
 */
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return template -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                template.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
            }
        };
    }
}

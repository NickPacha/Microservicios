package com.sistema.transporte.gateway.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Seguridad del perimetro (gateway, WebFlux):
 *
 *  - /dev/** queda abierto: es el registro/login que emite el token.
 *  - Todo /api/** exige un JWT valido (firma + expiracion) ANTES de
 *    llegar a los microservicios. Un token invalido muere aqui, en el
 *    borde, sin consumir recursos internos.
 *  - El RBAC fino (USER vs ADMIN, propietario del recurso) NO se hace
 *    aqui: es responsabilidad de cada microservicio (defensa en
 *    profundidad).
 *
 * La clave HS256 es la misma clave DEV compartida con ms-reserva.
 * En produccion ambos validarian contra el JWK Set de Keycloak
 * (issuer-uri) en lugar de una clave simetrica.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final SecretKey secretKey;

    public GatewaySecurityConfig(@Value("${app.security.dev-secret}") String devSecret) {
        this.secretKey = new SecretKeySpec(
                devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                // Login/registro dev y health checks: abiertos
                .pathMatchers("/dev/**").permitAll()
                .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Todo lo demas exige JWT valido
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /** Decodificador reactivo con la clave simetrica DEV. */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}

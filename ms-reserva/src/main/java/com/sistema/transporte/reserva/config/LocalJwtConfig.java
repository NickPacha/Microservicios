package com.sistema.transporte.reserva.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * SOLO PERFIL "local": reemplaza la validacion contra Keycloak por una
 * clave simetrica HS256, para poder probar todo el flujo de seguridad
 * (JWT + RBAC) sin infraestructura externa.
 *
 * Al declarar un JwtDecoder propio, la autoconfiguracion de Spring Boot
 * (issuer-uri / jwk-set-uri) se desactiva automaticamente.
 *
 * NUNCA activar este perfil en produccion.
 */
@Configuration
@Profile("local")
public class LocalJwtConfig {

    private final SecretKey secretKey;

    public LocalJwtConfig(@Value("${app.security.dev-secret}") String devSecret) {
        this.secretKey = new SecretKeySpec(
                devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Valida los tokens emitidos por DevTokenController. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Firma los tokens de prueba. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }
}

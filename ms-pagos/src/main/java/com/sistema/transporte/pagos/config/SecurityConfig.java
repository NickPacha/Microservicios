package com.sistema.transporte.pagos.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de ms-pagos (Resource Server).
 *
 * ANTES: este microservicio no tenia seguridad alguna. Cualquiera que
 * alcanzara el puerto 8084 (o lo enrutara por el gateway) podia hacer
 * POST /api/pagos/procesar con un monto y una reserva arbitrarios,
 * inyectando pagos falsos o marcando como pagadas reservas ajenas.
 *
 * AHORA: toda llamada a /api/pagos/** exige un JWT valido. ms-reserva
 * reenvia el token del usuario en su llamada Feign (defensa en
 * profundidad, coherente con el resto del sistema). El control de
 * PROPIEDAD de la reserva sigue en ms-reserva, que es quien conoce
 * la BD de reservas; aqui solo verificamos autenticacion.
 *
 * La clave HS256 es la misma clave DEV compartida con el gateway y
 * ms-reserva. En produccion se sustituiria por la validacion contra
 * el JWK Set de Keycloak (issuer-uri), sin tocar el resto del codigo.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecretKey secretKey;

    public SecurityConfig(@Value("${app.security.dev-secret}") String devSecret) {
        this.secretKey = new SecretKeySpec(
                devSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health checks de infraestructura (K8s / Docker healthcheck)
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Todo el contrato de pagos exige autenticacion
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }

    /**
     * Decodificador HS256 con la clave DEV compartida. Valida firma y
     * expiracion del token emitido por el sistema.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}

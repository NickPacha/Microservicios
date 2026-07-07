package com.sistema.transporte.reserva.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Unitario puro: mapeo de claims del JWT a authorities ROLE_*. */
class JwtRoleConverterTest {

    private final JwtRoleConverter converter = new JwtRoleConverter();

    @Test
    void convierteClaimRolesAPrefijoRole() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256")
                .claim("roles", List.of("admin", "user"))
                .build();

        var authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void soportaFormatoRealmAccessDeKeycloak() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256")
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .build();

        var authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }

    @Test
    void sinClaimsDeRolesDevuelveVacio() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256")
                .claim("sub", "fernando")
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}

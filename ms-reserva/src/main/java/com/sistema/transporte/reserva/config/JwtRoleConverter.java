package com.sistema.transporte.reserva.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Extrae los roles del JWT y los mapea a authorities con prefijo ROLE_
 * (formato que esperan hasRole(...) y @PreAuthorize("hasRole(...)")).
 *
 * Soporta dos formatos de claim:
 *  1. "roles": ["ADMIN", "USER"]                      -> emitido por un IdP propio
 *  2. "realm_access": { "roles": ["ADMIN", "USER"] }  -> formato Keycloak
 */
@Component
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<String> roles = List.of();

        if (jwt.hasClaim(CLAIM_ROLES)) {
            roles = jwt.getClaimAsStringList(CLAIM_ROLES);
        } else if (jwt.hasClaim(CLAIM_REALM_ACCESS)) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap(CLAIM_REALM_ACCESS);
            Object rawRoles = realmAccess.get(CLAIM_ROLES);
            if (rawRoles instanceof Collection<?> collection) {
                roles = (Collection<String>) collection;
            }
        }

        return roles.stream()
                .flatMap(role -> Stream.of((GrantedAuthority)
                        new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase())))
                .toList();
    }
}

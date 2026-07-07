package com.sistema.transporte.reserva.service.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Componente transversal para recuperar el contexto de seguridad
 * (usuario autenticado y roles) desde el SecurityContextHolder,
 * de forma que la capa de servicios no dependa de la capa web.
 */
@Component
public class AuthenticatedUserProvider {

    private static final String ROLE_PREFIX = "ROLE_";

    /** Username del token: claim preferred_username, o el subject (sub) como fallback. */
    public String getUsername() {
        Authentication auth = getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String preferred = jwt.getClaimAsString("preferred_username");
            return preferred != null ? preferred : jwt.getSubject();
        }
        return auth.getName();
    }

    /** Roles del usuario autenticado, sin el prefijo ROLE_. */
    public Set<String> getRoles() {
        return getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(ROLE_PREFIX))
                .map(a -> a.substring(ROLE_PREFIX.length()))
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String role) {
        return getRoles().contains(role.toUpperCase());
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /** Acceso directo a un claim arbitrario del JWT (ej. email, tenant). */
    public String getClaim(String claimName) {
        Authentication auth = getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsString(claimName);
        }
        return null;
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No existe un usuario autenticado en el contexto de seguridad");
        }
        return auth;
    }
}

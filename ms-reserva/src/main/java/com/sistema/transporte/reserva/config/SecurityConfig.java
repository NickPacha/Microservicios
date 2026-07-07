package com.sistema.transporte.reserva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de seguridad transversal del microservicio (Resource Server).
 *
 * Principio "confiar pero verificar": aunque el API Gateway (ms-lib-api-gateway)
 * ya valida el JWT en el perimetro, este microservicio vuelve a verificar la
 * firma contra el JWK Set del servidor de identidad y valida el issuer (iss).
 * Asi, un token falsificado no puede colarse aunque la red interna sea comprometida.
 *
 * - STATELESS: no se crean sesiones HTTP; el estado de autenticacion viaja en el JWT.
 * - CSRF deshabilitado: solo aplica a autenticacion basada en cookies/sesion.
 * - RBAC: reglas por metodo HTTP + refuerzo con @PreAuthorize en los controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize / @PostAuthorize en controllers y servicios
public class SecurityConfig {

    private final JwtRoleConverter jwtRoleConverter;

    public SecurityConfig(JwtRoleConverter jwtRoleConverter) {
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Stateless por JWT: sin sesiones ni CSRF
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 2. Politicas de autorizacion RBAC por endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoints de infraestructura (health checks de Eureka/K8s)
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                // Consola web de pruebas (recursos estaticos) y Swagger UI.
                // Solo exponen la interfaz: cada llamada a la API sigue
                // exigiendo un JWT valido (RBAC intacto).
                .requestMatchers("/", "/index.html", "/favicon.ico",
                        "/css/**", "/js/**").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**").permitAll()
                // Autenticacion DEV (registro, login, usuarios): el controller
                // solo existe en el perfil "local"; en produccion responde 404.
                .requestMatchers("/dev/**").permitAll()
                // Lectura: cualquier usuario autenticado con rol USER o ADMIN
                .requestMatchers(HttpMethod.GET, "/api/reservas/**").hasAnyRole("USER", "ADMIN")
                // Creacion: usuarios autenticados (un pasajero crea su propia reserva)
                .requestMatchers(HttpMethod.POST, "/api/reservas/**").hasAnyRole("USER", "ADMIN")
                // Pago: el propietario (USER) paga SU reserva; la propiedad
                // se verifica en la capa de servicio. Debe ir ANTES de la
                // regla generica de PUT (la primera coincidencia gana).
                .requestMatchers(HttpMethod.PUT, "/api/reservas/*/pagar").hasAnyRole("USER", "ADMIN")
                // Modificacion y borrado: solo ADMIN
                .requestMatchers(HttpMethod.PUT, "/api/reservas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/reservas/**").hasRole("ADMIN")
                // Todo lo demas requiere autenticacion
                .anyRequest().authenticated())

            // 3. Resource Server OAuth2: valida firma, expiracion e issuer del JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Convierte los claims del JWT en authorities de Spring Security,
     * delegando la extraccion de roles en JwtRoleConverter.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtRoleConverter);
        return converter;
    }
}

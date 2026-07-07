package com.sistema.transporte.reserva.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

/**
 * SOLO PERFIL "local": gestion de usuarios de desarrollo + emision de JWT,
 * todo dentro del mismo microservicio y puerto (8083), para que la consola
 * web (index.html) sea una sola pagina: registrar usuarios, iniciar sesion
 * y gestionar reservas, sin Keycloak.
 *
 * Los usuarios viven en memoria (se pierden al reiniciar, igual que la BD H2)
 * y las contrasenas se guardan con hash BCrypt. En produccion este bean no
 * existe: la identidad la gestiona Keycloak.
 */
@RestController
@RequestMapping("/dev")
@Profile("local")
public class DevAuthController {

    private static final long EXPIRACION_SEGUNDOS = 3600; // 1 hora

    private final JwtEncoder jwtEncoder;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Map<String, UsuarioDev> usuarios = new ConcurrentHashMap<>();

    public DevAuthController(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
        // Usuarios semilla para probar de inmediato
        registrarInterno("fernando", "fernando123", List.of("USER"));
        registrarInterno("admin", "admin123", List.of("ADMIN"));
    }

    private record UsuarioDev(String username, String passwordHash, List<String> roles) {}

    @Data
    public static class RegistroRequest {
        private String usuario;
        private String password;
        private List<String> roles; // ["USER"] o ["ADMIN"]
    }

    @Data
    public static class LoginRequest {
        private String usuario;
        private String password;
    }

    // ==================== USUARIOS ====================

    /** Registrar un usuario de prueba. */
    @PostMapping("/usuarios")
    public ResponseEntity<?> registrar(@RequestBody RegistroRequest req) {
        if (req.getUsuario() == null || req.getUsuario().isBlank()
                || req.getPassword() == null || req.getPassword().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "usuario obligatorio y password de al menos 4 caracteres"));
        }
        String username = req.getUsuario().trim().toLowerCase();
        if (usuarios.containsKey(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "El usuario '" + username + "' ya existe"));
        }
        List<String> roles = (req.getRoles() == null || req.getRoles().isEmpty())
                ? List.of("USER") : req.getRoles().stream().map(String::toUpperCase).toList();
        registrarInterno(username, req.getPassword(), roles);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("usuario", username, "roles", roles));
    }

    /** Listar usuarios registrados (sin contrasenas). */
    @GetMapping("/usuarios")
    public List<Map<String, Object>> listar() {
        return usuarios.values().stream()
                .map(u -> Map.<String, Object>of("usuario", u.username(), "roles", u.roles()))
                .sorted((a, b) -> a.get("usuario").toString().compareTo(b.get("usuario").toString()))
                .toList();
    }

    /** Eliminar un usuario de prueba. */
    @DeleteMapping("/usuarios/{username}")
    public ResponseEntity<Void> eliminar(@PathVariable String username) {
        return usuarios.remove(username.toLowerCase()) != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ==================== LOGIN ====================

    /** Login con usuario y contrasena: devuelve el JWT con los roles del usuario. */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        UsuarioDev usuario = req.getUsuario() == null
                ? null : usuarios.get(req.getUsuario().trim().toLowerCase());
        if (usuario == null || req.getPassword() == null
                || !passwordEncoder.matches(req.getPassword(), usuario.passwordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Usuario o contrasena incorrectos"));
        }
        return ResponseEntity.ok(emitirToken(usuario.username(), usuario.roles()));
    }

    // ==================== INTERNOS ====================

    private void registrarInterno(String username, String password, List<String> roles) {
        usuarios.put(username, new UsuarioDev(username, passwordEncoder.encode(password), roles));
    }

    private Map<String, Object> emitirToken(String usuario, List<String> roles) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ms-reserva-local")
                .subject(usuario)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(EXPIRACION_SEGUNDOS))
                .claim("preferred_username", usuario)
                .claim("roles", roles) // formato soportado por JwtRoleConverter
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        return Map.of(
                "access_token", token,
                "token_type", "Bearer",
                "expires_in", EXPIRACION_SEGUNDOS,
                "usuario", usuario,
                "roles", roles);
    }
}

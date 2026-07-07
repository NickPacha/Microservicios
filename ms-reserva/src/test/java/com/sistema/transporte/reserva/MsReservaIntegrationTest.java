package com.sistema.transporte.reserva;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integracion end-to-end con el perfil "local" (H2 + auth dev):
 * arranca la aplicacion real y ejercita registro, login, RBAC y CRUD.
 * Sin ms-pagos corriendo, el Circuit Breaker deja la reserva PENDIENTE.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
class MsReservaIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    // ---------- utilitarios ----------

    private HttpHeaders headersConToken(String usuario, String password) {
        ResponseEntity<Map> login = rest.postForEntity("/dev/login",
                Map.of("usuario", usuario, "password", password), Map.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth((String) login.getBody().get("access_token"));
        return headers;
    }

    private Map<String, Object> reservaValida() {
        return Map.of(
                "origen", "Lima",
                "destino", "Cusco",
                "fechaViaje", LocalDateTime.now().plusDays(2)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                "asiento", "7B",
                "precio", 120.00);
    }

    // ---------- seguridad ----------

    @Test
    void sinTokenLaApiDevuelve401() {
        ResponseEntity<String> r = rest.getForEntity("/api/reservas", String.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() {
        ResponseEntity<Map> r = rest.postForEntity("/dev/login",
                Map.of("usuario", "fernando", "password", "incorrecta"), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registroDuplicadoDevuelve409() {
        rest.postForEntity("/dev/usuarios",
                Map.of("usuario", "repetido", "password", "clave12345"), Map.class);
        ResponseEntity<Map> r = rest.postForEntity("/dev/usuarios",
                Map.of("usuario", "repetido", "password", "clave12345"), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void registroConPasswordCortaDevuelve400() {
        ResponseEntity<Map> r = rest.postForEntity("/dev/usuarios",
                Map.of("usuario", "clavecorta", "password", "1234567"), Map.class);
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---------- flujo de negocio + RBAC ----------

    @Test
    void flujoCompletoUsuarioYAdmin() {
        HttpHeaders user = headersConToken("fernando", "fernando123");
        HttpHeaders admin = headersConToken("admin", "admin123");

        // Validacion: datos invalidos -> 400
        ResponseEntity<Map> invalida = rest.postForEntity("/api/reservas",
                new HttpEntity<>(Map.of("origen", "", "precio", -1), user), Map.class);
        assertThat(invalida.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // USER crea: 201, propietario del JWT, PENDIENTE (sin ms-pagos -> fallback CB)
        ResponseEntity<Map> creada = rest.postForEntity("/api/reservas",
                new HttpEntity<>(reservaValida(), user), Map.class);
        assertThat(creada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creada.getBody().get("usuario")).isEqualTo("fernando");
        assertThat(creada.getBody().get("estado")).isEqualTo("PENDIENTE");
        Integer id = (Integer) creada.getBody().get("id");

        // USER lee su propia reserva
        ResponseEntity<Map> propia = rest.exchange("/api/reservas/" + id,
                HttpMethod.GET, new HttpEntity<>(user), Map.class);
        assertThat(propia.getStatusCode()).isEqualTo(HttpStatus.OK);

        // USER no puede eliminar (RBAC de endpoint): 403
        ResponseEntity<Map> deleteUser = rest.exchange("/api/reservas/" + id,
                HttpMethod.DELETE, new HttpEntity<>(user), Map.class);
        assertThat(deleteUser.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // ADMIN confirma: 200 + CONFIRMADA
        ResponseEntity<Map> confirmada = rest.exchange("/api/reservas/" + id + "/confirmar",
                HttpMethod.PUT, new HttpEntity<>(admin), Map.class);
        assertThat(confirmada.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(confirmada.getBody().get("estado")).isEqualTo("CONFIRMADA");

        // ADMIN elimina: 204
        ResponseEntity<Void> eliminada = rest.exchange("/api/reservas/" + id,
                HttpMethod.DELETE, new HttpEntity<>(admin), Void.class);
        assertThat(eliminada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void usuarioNoAccedeAReservaAjena() {
        HttpHeaders fernando = headersConToken("fernando", "fernando123");
        rest.postForEntity("/dev/usuarios",
                Map.of("usuario", "intruso", "password", "clave12345"), Map.class);
        HttpHeaders intruso = headersConToken("intruso", "clave12345");

        ResponseEntity<Map> creada = rest.postForEntity("/api/reservas",
                new HttpEntity<>(reservaValida(), fernando), Map.class);
        Integer id = (Integer) creada.getBody().get("id");

        ResponseEntity<Map> ajena = rest.exchange("/api/reservas/" + id,
                HttpMethod.GET, new HttpEntity<>(intruso), Map.class);
        assertThat(ajena.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}

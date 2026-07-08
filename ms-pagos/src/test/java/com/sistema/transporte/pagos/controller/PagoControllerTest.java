package com.sistema.transporte.pagos.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PagoControllerTest {

    @Autowired
    private MockMvc mvc;

    private static final String PAGO = """
            {"reservaId": 10, "usuario": "fernando", "monto": 85.50}
            """;

    @Test
    void procesaElPagoYEsIdempotente() throws Exception {
        // Primer cobro (con JWT valido simulado: ms-pagos ahora exige autenticacion)
        mvc.perform(post("/api/pagos/procesar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(PAGO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.reservaId").value(10));

        // Reintento con la misma reserva: NO debe duplicar el cobro
        mvc.perform(post("/api/pagos/procesar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON).content(PAGO))
                .andExpect(status().isOk());

        mvc.perform(get("/api/pagos").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reservaId == 10)]", hasSize(1)));
    }

    @Test
    void rechazaPagoInvalido() throws Exception {
        mvc.perform(post("/api/pagos/procesar").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usuario\": \"\", \"monto\": -5}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaPagoSinToken() throws Exception {
        // Sin JWT la peticion muere en el filtro de seguridad (401): ya no se
        // pueden inyectar pagos anonimos como antes.
        mvc.perform(post("/api/pagos/procesar")
                        .contentType(MediaType.APPLICATION_JSON).content(PAGO))
                .andExpect(status().isUnauthorized());
    }
}

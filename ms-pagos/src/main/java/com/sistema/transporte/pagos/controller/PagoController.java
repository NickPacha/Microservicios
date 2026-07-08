package com.sistema.transporte.pagos.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.transporte.pagos.model.Pago;
import com.sistema.transporte.pagos.repository.PagoRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrato consumido por ms-reserva (PagosClient / OpenFeign):
 * POST /api/pagos/procesar.
 *
 * Cambios frente a la version en memoria:
 *  - Requiere JWT valido (ver SecurityConfig): ya no acepta pagos anonimos.
 *  - Los pagos se PERSISTEN con UNIQUE(reserva_id): la idempotencia sobrevive
 *    a reinicios y a llamadas concurrentes (antes se perdia al reiniciar).
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoRepository pagoRepository;

    /** Cuerpo que envia ms-reserva (mismo contrato que su PagoDTO). */
    @Data
    public static class PagoRequest {
        @NotNull(message = "reservaId es obligatorio")
        private Long reservaId;
        @NotBlank(message = "usuario es obligatorio")
        private String usuario;
        @NotNull @DecimalMin(value = "0.01", message = "el monto debe ser mayor a 0")
        private BigDecimal monto;
    }

    /**
     * Procesa el pago de una reserva. IDEMPOTENTE: si la reserva ya fue
     * pagada, devuelve el pago original en vez de cobrar dos veces
     * (protege contra dobles clics y reintentos del circuit breaker).
     * La restriccion UNIQUE(reserva_id) es la salvaguarda ante concurrencia.
     */
    @PostMapping("/procesar")
    public ResponseEntity<Pago> procesar(@Valid @RequestBody PagoRequest request) {
        Pago existente = pagoRepository.findByReservaId(request.getReservaId()).orElse(null);
        if (existente != null) {
            return ResponseEntity.ok(existente);
        }
        try {
            Pago nuevo = pagoRepository.save(Pago.builder()
                    .reservaId(request.getReservaId())
                    .usuario(request.getUsuario())
                    .monto(request.getMonto())
                    .estado("PAGADO")
                    .procesadoEn(LocalDateTime.now())
                    .build());
            log.info("Pago procesado: reserva={} usuario={} monto={}",
                    nuevo.getReservaId(), nuevo.getUsuario(), nuevo.getMonto());
            return ResponseEntity.ok(nuevo);
        } catch (DataIntegrityViolationException e) {
            // Carrera: otra peticion cobro la misma reserva entre el SELECT y el
            // INSERT. La UNIQUE lo impide; devolvemos el pago que si persistio.
            return ResponseEntity.ok(
                    pagoRepository.findByReservaId(request.getReservaId()).orElseThrow(() -> e));
        }
    }

    /** Listado de pagos procesados (para inspeccionar en el navegador/Postman). */
    @GetMapping
    public List<Pago> listar() {
        return pagoRepository.findAll();
    }
}

package com.sistema.transporte.pagos.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrato consumido por ms-reserva (PagosClient / OpenFeign):
 * POST /api/pagos/procesar. Los pagos se guardan en memoria.
 */
@Slf4j
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final AtomicLong secuencia = new AtomicLong(1);
    private final List<PagoProcesado> pagos = new CopyOnWriteArrayList<>();

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

    @Data
    @Builder
    public static class PagoProcesado {
        private Long id;
        private Long reservaId;
        private String usuario;
        private BigDecimal monto;
        private String estado;
        private LocalDateTime procesadoEn;
    }

    /** Procesa el pago de una reserva. */
    @PostMapping("/procesar")
    public ResponseEntity<PagoProcesado> procesar(@Valid @RequestBody PagoRequest request) {
        PagoProcesado pago = PagoProcesado.builder()
                .id(secuencia.getAndIncrement())
                .reservaId(request.getReservaId())
                .usuario(request.getUsuario())
                .monto(request.getMonto())
                .estado("PAGADO")
                .procesadoEn(LocalDateTime.now())
                .build();
        pagos.add(pago);
        log.info("Pago procesado: reserva={} usuario={} monto={}",
                pago.getReservaId(), pago.getUsuario(), pago.getMonto());
        return ResponseEntity.ok(pago);
    }

    /** Listado de pagos procesados (para inspeccionar en el navegador/Postman). */
    @GetMapping
    public List<PagoProcesado> listar() {
        return pagos;
    }
}

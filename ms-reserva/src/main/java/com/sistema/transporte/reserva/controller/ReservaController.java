package com.sistema.transporte.reserva.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.transporte.reserva.dto.ReservaRequestDTO;
import com.sistema.transporte.reserva.dto.ReservaResponseDTO;
import com.sistema.transporte.reserva.service.ReservaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints REST de reservas, expuestos detras del API Gateway.
 *
 * Doble capa de autorizacion (defensa en profundidad):
 *  1. SecurityConfig define reglas por metodo HTTP (filter chain).
 *  2. @PreAuthorize refuerza el RBAC de forma declarativa por metodo,
 *     de modo que la regla viaja junto al codigo del endpoint.
 */
@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaService reservaService;

    /** Lectura: USER (solo sus reservas, filtrado en el servicio) o ADMIN (todas). */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ReservaResponseDTO>> listar() {
        return ResponseEntity.ok(reservaService.listar());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReservaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    /** Creacion: cualquier usuario autenticado crea SU propia reserva. */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReservaResponseDTO> crear(@Valid @RequestBody ReservaRequestDTO request) {
        ReservaResponseDTO creada = reservaService.crear(request);
        return ResponseEntity.created(URI.create("/api/reservas/" + creada.getId())).body(creada);
    }

    /**
     * Pago de la reserva por su PROPIETARIO (o un ADMIN): reintenta el cobro
     * de una reserva PENDIENTE. El control de propiedad (que un USER solo
     * pague SU reserva) se aplica en la capa de servicio.
     */
    @PutMapping("/{id}/pagar")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ReservaResponseDTO> pagar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.pagar(id));
    }

    /** Confirmacion manual: operacion administrativa. */
    @PutMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservaResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.confirmar(id));
    }

    /** Borrado: solo ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reservaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

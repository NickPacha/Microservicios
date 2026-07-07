package com.sistema.transporte.reserva.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.transporte.reserva.client.PagosClient;
import com.sistema.transporte.reserva.dto.PagoDTO;
import com.sistema.transporte.reserva.dto.ReservaRequestDTO;
import com.sistema.transporte.reserva.dto.ReservaResponseDTO;
import com.sistema.transporte.reserva.exception.ReservaNoEncontradaException;
import com.sistema.transporte.reserva.model.EstadoReserva;
import com.sistema.transporte.reserva.model.Reserva;
import com.sistema.transporte.reserva.repository.ReservaRepository;
import com.sistema.transporte.reserva.service.ReservaService;
import com.sistema.transporte.reserva.service.security.AuthenticatedUserProvider;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacion de la logica de negocio.
 *
 * Seguridad aplicada en esta capa (defensa en profundidad):
 *  - El usuario propietario se toma del JWT (AuthenticatedUserProvider),
 *    nunca del body de la peticion.
 *  - RBAC a nivel de DATOS: un USER solo ve/gestiona SUS reservas;
 *    un ADMIN ve todas (el RBAC a nivel de ENDPOINT lo aplican
 *    SecurityConfig y @PreAuthorize en el controller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final PagosClient pagosClient;
    private final AuthenticatedUserProvider authenticatedUser;

    /** Tope de pagina para no permitir descargar la tabla completa. */
    private static final int MAX_PAGE_SIZE = 200;

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponseDTO> listar(int page, int size) {
        // Paginado + orden estable (mas recientes primero).
        // ADMIN lista todo; USER solo lo suyo (RBAC a nivel de datos).
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Reserva> reservas = authenticatedUser.isAdmin()
                ? reservaRepository.findAll(pageable)
                : reservaRepository.findByUsuario(authenticatedUser.getUsername(), pageable);
        return reservas.map(ReservaResponseDTO::fromEntity).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponseDTO obtenerPorId(Long id) {
        return ReservaResponseDTO.fromEntity(buscarConControlDeAcceso(id));
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "pagosCB", fallbackMethod = "crearSinPago")
    public ReservaResponseDTO crear(ReservaRequestDTO request) {
        String usuario = authenticatedUser.getUsername();
        log.info("Creando reserva para usuario={} destino={}", usuario, request.getDestino());

        Reserva reserva = reservaRepository.save(Reserva.builder()
                .usuario(usuario) // propietario tomado del JWT, no del cliente
                .origen(request.getOrigen())
                .destino(request.getDestino())
                .fechaViaje(request.getFechaViaje())
                .asiento(request.getAsiento())
                .precio(request.getPrecio())
                .estado(EstadoReserva.PENDIENTE)
                .build());

        // Comunicacion sincronica con ms-pagos, protegida por Circuit Breaker
        pagosClient.procesarPago(PagoDTO.builder()
                .reservaId(reserva.getId())
                .usuario(usuario)
                .monto(reserva.getPrecio())
                .build());

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return ReservaResponseDTO.fromEntity(reserva);
    }

    /**
     * Fallback del Circuit Breaker: si ms-pagos no responde, la reserva
     * queda PENDIENTE (degradacion elegante) en lugar de fallar la peticion.
     * Un proceso asincrono (evento Kafka/RabbitMQ) puede reintentarla despues.
     */
    @Transactional
    public ReservaResponseDTO crearSinPago(ReservaRequestDTO request, Throwable causa) {
        log.warn("ms-pagos no disponible ({}). Reserva creada en estado PENDIENTE.", causa.getMessage());
        Reserva reserva = reservaRepository.save(Reserva.builder()
                .usuario(authenticatedUser.getUsername())
                .origen(request.getOrigen())
                .destino(request.getDestino())
                .fechaViaje(request.getFechaViaje())
                .asiento(request.getAsiento())
                .precio(request.getPrecio())
                .estado(EstadoReserva.PENDIENTE)
                .build());
        return ReservaResponseDTO.fromEntity(reserva);
    }

    /**
     * Pago iniciado por el PROPIETARIO de la reserva (o un ADMIN):
     * reintenta el cobro de una reserva PENDIENTE contra ms-pagos.
     * Si el pago se procesa, pasa a CONFIRMADA; si ms-pagos sigue caido,
     * el fallback la deja PENDIENTE (sin romper la peticion).
     */
    @Override
    @Transactional
    @CircuitBreaker(name = "pagosCB", fallbackMethod = "pagarSinPago")
    public ReservaResponseDTO pagar(Long id) {
        Reserva reserva = buscarConControlDeAcceso(id);
        if (reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            return ReservaResponseDTO.fromEntity(reserva); // ya pagada
        }
        pagosClient.procesarPago(PagoDTO.builder()
                .reservaId(reserva.getId())
                .usuario(reserva.getUsuario())
                .monto(reserva.getPrecio())
                .build());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        log.info("Usuario={} pago la reserva id={}", authenticatedUser.getUsername(), id);
        return ReservaResponseDTO.fromEntity(reserva);
    }

    /**
     * Fallback de pagar(): NO debe tragarse los errores de negocio/seguridad
     * (404, 403); solo aplica cuando ms-pagos esta caido.
     */
    @Transactional
    public ReservaResponseDTO pagarSinPago(Long id, Throwable causa) {
        if (causa instanceof AccessDeniedException ade) throw ade;
        if (causa instanceof ReservaNoEncontradaException rne) throw rne;
        log.warn("ms-pagos no disponible ({}). La reserva id={} sigue PENDIENTE.",
                causa.getMessage(), id);
        return ReservaResponseDTO.fromEntity(buscarConControlDeAcceso(id));
    }

    @Override
    @Transactional
    public ReservaResponseDTO confirmar(Long id) {
        Reserva reserva = buscarConControlDeAcceso(id);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return ReservaResponseDTO.fromEntity(reserva);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Reserva reserva = buscarConControlDeAcceso(id);
        log.info("Usuario={} elimina reserva id={}", authenticatedUser.getUsername(), id);
        reservaRepository.delete(reserva);
    }

    /** Regla transversal: USER solo accede a sus propias reservas; ADMIN a todas. */
    private Reserva buscarConControlDeAcceso(Long id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(id));
        if (!authenticatedUser.isAdmin()
                && !reserva.getUsuario().equals(authenticatedUser.getUsername())) {
            throw new AccessDeniedException("La reserva no pertenece al usuario autenticado");
        }
        return reserva;
    }
}

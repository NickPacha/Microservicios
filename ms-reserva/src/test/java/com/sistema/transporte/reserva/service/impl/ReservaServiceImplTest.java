package com.sistema.transporte.reserva.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import com.sistema.transporte.reserva.client.PagosClient;
import com.sistema.transporte.reserva.dto.ReservaRequestDTO;
import com.sistema.transporte.reserva.dto.ReservaResponseDTO;
import com.sistema.transporte.reserva.exception.ReservaNoEncontradaException;
import com.sistema.transporte.reserva.model.EstadoReserva;
import com.sistema.transporte.reserva.model.Reserva;
import com.sistema.transporte.reserva.repository.ReservaRepository;
import com.sistema.transporte.reserva.service.security.AuthenticatedUserProvider;

/**
 * Unitarios con Mockito de la logica de negocio y del RBAC a nivel de datos.
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock private ReservaRepository reservaRepository;
    @Mock private PagosClient pagosClient;
    @Mock private AuthenticatedUserProvider authenticatedUser;

    @InjectMocks private ReservaServiceImpl service;

    private ReservaRequestDTO requestValido() {
        return ReservaRequestDTO.builder()
                .origen("Lima").destino("Cusco")
                .fechaViaje(LocalDateTime.now().plusDays(2))
                .asiento("7B").precio(new BigDecimal("120.00"))
                .build();
    }

    @Test
    void crearTomaElPropietarioDelJwtYConfirmaConPagoExitoso() {
        when(authenticatedUser.getUsername()).thenReturn("fernando");
        when(reservaRepository.save(any())).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(pagosClient.procesarPago(any())).thenReturn(ResponseEntity.ok("PAGADO"));

        ReservaResponseDTO creada = service.crear(requestValido());

        assertThat(creada.getUsuario()).isEqualTo("fernando"); // del JWT, no del body
        assertThat(creada.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void usuarioNoPropietarioRecibeAccessDenied() {
        when(authenticatedUser.isAdmin()).thenReturn(false);
        when(authenticatedUser.getUsername()).thenReturn("maria");
        Reserva ajena = Reserva.builder().id(5L).usuario("fernando").build();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> service.obtenerPorId(5L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminSiAccedeAReservaAjena() {
        when(authenticatedUser.isAdmin()).thenReturn(true);
        Reserva ajena = Reserva.builder().id(5L).usuario("fernando")
                .estado(EstadoReserva.PENDIENTE).build();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(ajena));

        assertThat(service.obtenerPorId(5L).getUsuario()).isEqualTo("fernando");
    }

    @Test
    void reservaInexistenteLanzaNoEncontrada() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(ReservaNoEncontradaException.class);
    }

    @Test
    void pagarReservaYaConfirmadaNoVuelveACobrar() {
        when(authenticatedUser.isAdmin()).thenReturn(true);
        Reserva confirmada = Reserva.builder().id(3L).usuario("fernando")
                .estado(EstadoReserva.CONFIRMADA).build();
        when(reservaRepository.findById(3L)).thenReturn(Optional.of(confirmada));

        ReservaResponseDTO resultado = service.pagar(3L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(pagosClient, never()).procesarPago(any()); // idempotente
    }
}

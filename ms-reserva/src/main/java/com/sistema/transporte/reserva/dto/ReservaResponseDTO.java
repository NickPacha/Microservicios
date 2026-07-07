package com.sistema.transporte.reserva.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sistema.transporte.reserva.model.EstadoReserva;
import com.sistema.transporte.reserva.model.Reserva;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** DTO de salida: proyeccion controlada de la entidad hacia el cliente. */
@Getter
@Builder
@AllArgsConstructor
public class ReservaResponseDTO {

    private Long id;
    private String usuario;
    private String origen;
    private String destino;
    private LocalDateTime fechaViaje;
    private String asiento;
    private BigDecimal precio;
    private EstadoReserva estado;
    private LocalDateTime creadoEn;

    public static ReservaResponseDTO fromEntity(Reserva reserva) {
        return ReservaResponseDTO.builder()
                .id(reserva.getId())
                .usuario(reserva.getUsuario())
                .origen(reserva.getOrigen())
                .destino(reserva.getDestino())
                .fechaViaje(reserva.getFechaViaje())
                .asiento(reserva.getAsiento())
                .precio(reserva.getPrecio())
                .estado(reserva.getEstado())
                .creadoEn(reserva.getCreadoEn())
                .build();
    }
}

package com.sistema.transporte.reserva.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contrato de comunicacion sincronica (OpenFeign) hacia ms-pagos. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {

    private Long reservaId;
    private String usuario;
    private BigDecimal monto;
}

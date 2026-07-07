package com.sistema.transporte.reserva.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada. Nunca exponemos la entidad JPA directamente:
 * el DTO define el contrato publico y aplica validacion declarativa.
 * Nota: NO incluye el campo "usuario"; ese dato se toma SIEMPRE del JWT
 * para impedir suplantacion (mass assignment / IDOR).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequestDTO {

    @NotBlank(message = "El origen es obligatorio")
    @Size(max = 120)
    private String origen;

    @NotBlank(message = "El destino es obligatorio")
    @Size(max = 120)
    private String destino;

    @NotNull(message = "La fecha de viaje es obligatoria")
    @Future(message = "La fecha de viaje debe ser futura")
    private LocalDateTime fechaViaje;

    @NotBlank(message = "El asiento es obligatorio")
    @Size(max = 10)
    private String asiento;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
    private BigDecimal precio;
}

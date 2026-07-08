package com.sistema.transporte.pagos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pago persistido. La restriccion UNIQUE sobre reserva_id garantiza la
 * IDEMPOTENCIA a nivel de base de datos: una reserva no puede cobrarse dos
 * veces ni siquiera ante llamadas concurrentes o reintentos del circuit
 * breaker. Antes esto vivia solo en memoria y se perdia en cada reinicio.
 */
@Entity
@Table(name = "pago", uniqueConstraints = @UniqueConstraint(
        name = "uk_pago_reserva", columnNames = "reserva_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "procesado_en", nullable = false, updatable = false)
    private LocalDateTime procesadoEn;

    @PrePersist
    void prePersist() {
        if (this.procesadoEn == null) {
            this.procesadoEn = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = "PAGADO";
        }
    }
}

package com.sistema.transporte.reserva.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA mapeada a la tabla "reserva" en PostgreSQL.
 * Esquema versionado con Flyway (V1__crear_tabla_reserva.sql).
 */
@Entity
@Table(name = "reserva")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Username del pasajero (extraido del JWT, nunca del body de la peticion). */
    @Column(name = "usuario", nullable = false, length = 100)
    private String usuario;

    @Column(name = "origen", nullable = false, length = 120)
    private String origen;

    @Column(name = "destino", nullable = false, length = 120)
    private String destino;

    @Column(name = "fecha_viaje", nullable = false)
    private LocalDateTime fechaViaje;

    @Column(name = "asiento", nullable = false, length = 10)
    private String asiento;

    @Column(name = "precio", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        this.creadoEn = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoReserva.PENDIENTE;
        }
    }
}

package com.sistema.transporte.pagos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sistema.transporte.pagos.model.Pago;

/**
 * Repositorio de pagos. findByReservaId da soporte a la idempotencia:
 * antes de cobrar comprobamos si la reserva ya tiene un pago; la restriccion
 * UNIQUE de la entidad es la salvaguarda final ante concurrencia.
 */
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReservaId(Long reservaId);
}

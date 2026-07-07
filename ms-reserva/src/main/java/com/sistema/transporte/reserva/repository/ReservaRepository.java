package com.sistema.transporte.reserva.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.transporte.reserva.model.Reserva;

/**
 * Repositorio Spring Data JPA. Persistencia independiente:
 * este microservicio es el UNICO que accede a la base "reserva_db".
 */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Consulta derivada: reservas del usuario autenticado (para RBAC a nivel de datos). */
    List<Reserva> findByUsuario(String usuario);
}

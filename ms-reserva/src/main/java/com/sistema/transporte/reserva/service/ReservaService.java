package com.sistema.transporte.reserva.service;

import java.util.List;

import com.sistema.transporte.reserva.dto.ReservaRequestDTO;
import com.sistema.transporte.reserva.dto.ReservaResponseDTO;

/** Contrato de la logica de negocio de reservas. */
public interface ReservaService {

    List<ReservaResponseDTO> listar(int page, int size);

    ReservaResponseDTO obtenerPorId(Long id);

    ReservaResponseDTO crear(ReservaRequestDTO request);

    ReservaResponseDTO pagar(Long id);

    ReservaResponseDTO confirmar(Long id);

    void eliminar(Long id);
}

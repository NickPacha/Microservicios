package com.sistema.transporte.reserva.exception;

/** Se lanza cuando la reserva solicitada no existe. Mapeada a HTTP 404. */
public class ReservaNoEncontradaException extends RuntimeException {

    public ReservaNoEncontradaException(Long id) {
        super("No existe la reserva con id " + id);
    }
}

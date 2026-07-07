package com.sistema.transporte.reserva.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.sistema.transporte.reserva.dto.PagoDTO;

/**
 * Cliente OpenFeign hacia ms-pagos, resuelto por nombre logico via Eureka.
 *
 * El Circuit Breaker (@CircuitBreaker de Resilience4j) se aplica en la capa
 * de servicio (ReservaServiceImpl) y no aqui: el aspecto AOP de Resilience4j
 * intercepta beans de Spring gestionados por proxy, y colocarlo sobre la
 * interfaz Feign con un metodo default como fallback no es interceptado de
 * forma fiable. Mantener la resiliencia en el servicio ademas permite que el
 * fallback aplique logica de negocio (ej. dejar la reserva en PENDIENTE).
 */
@FeignClient(name = "ms-pagos", path = "/api/pagos")
public interface PagosClient {

    @PostMapping("/procesar")
    ResponseEntity<String> procesarPago(@RequestBody PagoDTO pago);
}

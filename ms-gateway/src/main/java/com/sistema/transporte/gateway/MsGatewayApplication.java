package com.sistema.transporte.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway del sistema de transporte (puerto 8080).
 *
 * Responsabilidades:
 *  - Punto de entrada unico: enruta /api/reservas -> ms-reserva (8083)
 *    y /api/pagos -> ms-pagos (8084).
 *  - Seguridad en el perimetro: valida el JWT ANTES de enrutar
 *    (GatewaySecurityConfig). Los microservicios lo vuelven a validar
 *    ("confiar pero verificar") y aplican el RBAC fino.
 *
 * En esta version local las rutas son estaticas (localhost). En un
 * entorno completo se resolverian por Eureka (uri: lb://ms-reserva).
 */
@SpringBootApplication
public class MsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsGatewayApplication.class, args);
    }
}

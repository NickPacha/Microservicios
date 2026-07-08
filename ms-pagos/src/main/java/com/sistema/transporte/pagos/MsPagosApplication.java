package com.sistema.transporte.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-pagos: recibe los pagos que ms-reserva le envia via OpenFeign y los
 * procesa, de modo que las reservas pasen a CONFIRMADA en lugar de PENDIENTE.
 *
 * Endurecido respecto a la version inicial "en memoria":
 *  - Seguridad JWT (Resource Server): rechaza pagos sin token valido.
 *  - Persistencia con UNIQUE(reserva_id): idempotencia durable (sobrevive
 *    a reinicios y a llamadas concurrentes).
 *
 * Pendiente hacia produccion: validar contra Keycloak (issuer-uri) en vez
 * de la clave HS256 de desarrollo, y PostgreSQL en lugar de H2.
 */
@SpringBootApplication
public class MsPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPagosApplication.class, args);
    }
}

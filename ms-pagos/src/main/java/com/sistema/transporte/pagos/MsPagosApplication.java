package com.sistema.transporte.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-pagos: version minima para desarrollo local.
 * Recibe los pagos que ms-reserva le envia via OpenFeign y los "procesa"
 * (los guarda en memoria y responde OK), de modo que las reservas pasen
 * a CONFIRMADA en lugar de quedar PENDIENTE.
 *
 * En un entorno real este MS tendria su propia BD, seguridad JWT,
 * Eureka, etc., igual que ms-reserva.
 */
@SpringBootApplication
public class MsPagosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPagosApplication.class, args);
    }
}

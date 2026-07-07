package com.sistema.transporte.reserva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Punto de entrada del microservicio ms-reserva.
 *
 * - @EnableDiscoveryClient: registra la instancia en Eureka (ms-lib-registry-server).
 * - @EnableFeignClients: habilita el escaneo de interfaces @FeignClient en client/.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class MsReservaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsReservaApplication.class, args);
    }
}

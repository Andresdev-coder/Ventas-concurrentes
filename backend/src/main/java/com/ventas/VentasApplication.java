package com.ventas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada de Spring Boot: configura componentes y arranca el servidor HTTP. */
@SpringBootApplication
public class VentasApplication {
    public static void main(String[] args) {
        // Al iniciar, Spring crea controladores/servicios y configura PostgreSQL.
        SpringApplication.run(VentasApplication.class, args);
    }
}
